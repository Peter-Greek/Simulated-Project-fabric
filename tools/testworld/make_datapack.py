"""Generates the Simulated test-bench datapack, and validates it before writing.

Every block id and blockstate property it emits is checked against the real
blockstate JSONs -- the mod's own generated tree for `simulated:`, and the Create
jar on the compile classpath for `create:`. A typo becomes a build-time failure
here rather than a silent `/setblock` error in chat.

    python tools/testworld/make_datapack.py            # validate + write to build/
    python tools/testworld/make_datapack.py --install   # also copy into the world

The pack is inert: nothing happens until `/function simtest:...` is run.
"""
import argparse
import json
import os
import shutil
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SIM_BS = ROOT / 'simulated/fabric/src/main/generated/assets/simulated/blockstates'
OUT = ROOT / 'build/testworld/simulated_testbench'
WORLD = Path(r'C:\Users\xerxe\curseforge\minecraft\Instances\Homestead - A Cozy Survival Experience\saves\テスト')

FLOOR = 200          # platform surface
Y = FLOOR + 1        # blocks sit here

# ---------------------------------------------------------------- validation

_states = {}
_items = set()


def load_states():
    """block id -> {property: {valid values}}; empty dict means "no properties"."""
    for f in SIM_BS.glob('*.json'):
        _states['simulated:' + f.stem] = _props(json.loads(f.read_text(encoding='utf-8')))
    # Registrate writes one item model per BlockItem, so the generated item model
    # tree is exactly the set of things /give accepts. Blocks deliberately
    # registered without .item() -- merging glue, the fifteen dyed symmetric sails
    # -- correctly do not appear.
    for f in (SIM_BS.parent / 'models/item').glob('*.json'):
        _items.add('simulated:' + f.stem)

    # Create ships its blockstates in its jar; read them from the compile classpath.
    jar = None
    for cand in (ROOT / '.gradle/loom-cache/remapped_mods').rglob('create-fabric-*.jar'):
        if 'sources' not in cand.name:
            jar = cand
            break
    if jar is None:
        print('warning: Create jar not found; create: ids will not be validated')
        return
    with zipfile.ZipFile(jar) as z:
        for name in z.namelist():
            if name.startswith('assets/create/blockstates/') and name.endswith('.json'):
                bid = 'create:' + Path(name).stem
                try:
                    _states[bid] = _props(json.loads(z.read(name).decode('utf-8')))
                except Exception:
                    _states[bid] = {}
            elif name.startswith('assets/create/models/item/') and name.endswith('.json'):
                _items.add('create:' + Path(name).stem)


def _props(doc):
    out = {}
    if 'variants' in doc:
        for key in doc['variants']:
            for part in key.split(','):
                if '=' in part:
                    a, b = part.split('=', 1)
                    out.setdefault(a, set()).add(b)
    for case in doc.get('multipart', []):
        when = case.get('when', {})
        for a, b in when.items():
            if a in ('OR', 'AND'):
                for sub in (b if isinstance(b, list) else [b]):
                    for a2, b2 in sub.items():
                        for v in str(b2).split('|'):
                            out.setdefault(a2, set()).add(v)
                continue
            for v in str(b).split('|'):
                out.setdefault(a, set()).add(v)
    return out


PROBLEMS = []
LEVER = "minecraft:lever[face=floor,facing=north]"

VANILLA_OK = {'minecraft:cobblestone', 'minecraft:air', 'minecraft:redstone_lamp',
              'minecraft:redstone_wire', 'minecraft:redstone_block', 'minecraft:chest',
              'minecraft:oak_sign', 'minecraft:lever', 'minecraft:stone', 'minecraft:glass',
              'minecraft:oak_planks', 'minecraft:piston', 'minecraft:comparator',
              'minecraft:repeater', 'minecraft:barrel', 'minecraft:hopper',
              'minecraft:slime_ball', 'minecraft:coal', 'minecraft:honeycomb'}


def block(bid, **props):
    """Validated block string for /setblock and /fill."""
    if bid.startswith('minecraft:'):
        if bid not in VANILLA_OK:
            PROBLEMS.append('unlisted vanilla block %s' % bid)
        return bid + _fmt(props)
    known = _states.get(bid)
    if known is None:
        PROBLEMS.append('unknown block id %s' % bid)
        return bid + _fmt(props)
    for k, v in props.items():
        if k not in known:
            PROBLEMS.append('%s has no property %s (has: %s)' % (bid, k, ','.join(sorted(known)) or 'none'))
        elif str(v) not in known[k]:
            PROBLEMS.append('%s %s=%s invalid (valid: %s)' % (bid, k, v, '|'.join(sorted(known[k]))))
    return bid + _fmt(props)


def item(iid):
    """Validated item id for /give. Vanilla is trusted; mod ids are checked."""
    if not iid.startswith('minecraft:') and iid not in _items:
        PROBLEMS.append('unknown item id %s' % iid)
    return iid


def _fmt(props):
    if not props:
        return ''
    return '[' + ','.join('%s=%s' % (k, str(v).lower()) for k, v in sorted(props.items())) + ']'


# ---------------------------------------------------------------- emit helpers

class Fn:
    def __init__(self, name, *header):
        self.name = name
        self.lines = ['# ' + h for h in header]

    def cmd(self, c):
        self.lines.append(c)
        return self

    def blank(self):
        self.lines.append('')
        return self

    def say(self, msg):
        return self.cmd('tellraw @s ' + json.dumps({'text': msg, 'color': 'aqua'}))

    def set(self, x, z, bid, y=Y, **props):
        return self.cmd('setblock %d %d %d %s' % (x, y, z, block(bid, **props)))

    def sign(self, x, z, text, y=Y):
        """A label block. 1.20 signs need exactly four `messages`, each a JSON
        text component; fewer than four renders blank. Single-quoted SNBT holds
        the JSON so nothing has to be double-escaped."""
        words, lines, cur = text.split(), [], ''
        for w in words:
            if len(cur) + len(w) + 1 > 15:
                lines.append(cur); cur = w
            else:
                cur = (cur + ' ' + w).strip()
        if cur:
            lines.append(cur)
        lines = (lines + ['', '', '', ''])[:4]
        msgs = ','.join("'" + json.dumps({'text': l}) + "'" for l in lines)
        return self.cmd('setblock %d %d %d minecraft:oak_sign[rotation=8]{front_text:{messages:[%s]}}'
                        % (x, y, z, msgs))


FUNCTIONS = []


def emit(fn):
    FUNCTIONS.append(fn)
    return fn


# ---------------------------------------------------------------- platform

def platform(name, half, note):
    fn = emit(Fn(name, note))
    fn.say('Building %dx%d platform at y=%d ...' % (half * 2, half * 2, FLOOR))
    step = 128
    for x0 in range(-half, half, step):
        for z0 in range(-half, half, step):
            x1, z1 = min(x0 + step - 1, half - 1), min(z0 + step - 1, half - 1)
            fn.cmd('fill %d %d %d %d %d %d %s' % (x0, FLOOR, z0, x1, FLOOR, z1, block('minecraft:cobblestone')))
    fn.say('Done. If parts are missing, chunks were not loaded -- raise render distance and re-run.')
    return fn


# ---------------------------------------------------------------- modules

def module_a():
    fn = emit(Fn('module_a', 'Redstone bench: throttle lever, accumulator, inductor,',
                 'magnets, linked receivers, typewriter.'))
    fn.sign(-1, 0, 'A REDSTONE')
    # Throttle lever drives the bus.
    fn.set(0, 0, 'simulated:throttle_lever', face='floor', facing='north', inverted='false')
    fn.cmd('fill 1 %d 0 3 %d 0 %s' % (Y, Y, block('minecraft:redstone_wire')))

    # Accumulator: back feed increases, side feed decreases.
    fn.set(4, 0, 'simulated:redstone_accumulator', facing='north')
    fn.set(5, 0, 'minecraft:redstone_lamp')
    fn.cmd('setblock 4 %d 1 %s' % (Y, LEVER))

    # Inductor: output drifts toward input.
    fn.set(7, 0, 'simulated:redstone_inductor', facing='north', inverted='false', powered='false')
    fn.set(8, 0, 'minecraft:redstone_lamp')

    # Two magnets, three apart, opposite poles -- should pull together.
    fn.set(11, 0, 'simulated:redstone_magnet', facing='north', powered='false')
    fn.set(14, 0, 'simulated:redstone_magnet', facing='south', powered='false')
    fn.set(11, 1, 'minecraft:redstone_block')

    # Receivers, with two Create links at different range/angle to read.
    fn.sign(-1, 3, 'A2 RECEIVERS')
    fn.set(4, 3, 'simulated:modulating_linked_receiver', facing='north', powered='false')
    fn.set(7, 3, 'simulated:directional_linked_receiver', facing='north', powered='false')
    fn.set(4, 9, 'create:redstone_link')        # near
    fn.set(20, 9, 'create:redstone_link')       # far -- modulating should read weaker
    fn.set(12, 6, 'create:redstone_link')       # off-axis -- directional should read weaker

    fn.sign(-1, 6, 'A3 TYPEWRITER')
    fn.set(0, 6, 'simulated:linked_typewriter', facing='north', powered='false')
    fn.set(2, 6, 'create:redstone_link')
    return fn


def module_b():
    fn = emit(Fn('module_b', 'Kinetic spine: portable engine -> analog transmission ->',
                 'directional gearshift -> auger shaft -> auger cog.'))
    fn.sign(-1, 12, 'B KINETICS')
    z = 12
    fn.set(0, z, 'simulated:white_portable_engine', facing='east', lit='false')
    fn.set(1, z, 'create:shaft', axis='x')
    fn.set(2, z, 'simulated:analog_transmission', axis='x', powered='false')
    fn.cmd('setblock 2 %d %d %s' % (Y, z + 1, LEVER))
    fn.set(3, z, 'create:shaft', axis='x')
    fn.set(4, z, 'simulated:directional_gearshift')      # default state; wrench to orient
    fn.set(5, z, 'create:shaft', axis='x')
    for x in range(6, 10):
        fn.set(x, z, 'simulated:auger_shaft', axis='x')
    fn.set(10, z, 'simulated:auger_cog', axis='x')
    # Item source and sink for the auger.
    fn.set(6, z - 1, 'minecraft:chest')
    fn.set(10, z - 1, 'minecraft:chest')
    fn.set(0, z - 1, 'minecraft:barrel')                 # fuel for the engine
    fn.blank()
    fn.sign(-1, 15, 'B2 SPARE')
    fn.set(0, 15, 'simulated:torsion_spring', facing='up', powered='false')
    fn.set(3, 15, 'simulated:swivel_bearing', facing='up', assembled='false', powered='false')
    fn.set(6, 15, 'simulated:white_symmetric_sail', axis='x')
    fn.set(9, 15, 'simulated:steering_wheel', facing='north', on_floor='true', waterlogged='false')
    return fn


def module_c():
    fn = emit(Fn('module_c', 'Sensors: optical + laser pair at ground level, and an',
                 'altitude/gimbal/velocity stack on a pillar.'))
    fn.sign(19, 0, 'C SENSORS')
    # Laser pointer fires north into a sensor six blocks away.
    fn.set(20, 0, 'simulated:laser_pointer', facing='north', inverted='false', powered='false')
    fn.cmd('setblock 20 %d 1 %s' % (Y, LEVER))
    fn.set(20, 7, 'simulated:laser_sensor', facing='south', powered='false')
    # Optical sensor watching a piston-driven block.
    fn.set(24, 0, 'simulated:optical_sensor', facing='north', powered='false')
    fn.set(24, 6, 'minecraft:piston')
    fn.cmd('setblock 24 %d 7 %s' % (Y, LEVER))
    # Pillar: velocity and gimbal low, altitude high enough to read a gradient.
    fn.sign(27, 0, 'C2 PILLAR')
    for y in range(Y, Y + 24):
        fn.cmd('setblock 28 %d 0 %s' % (y, block('minecraft:stone')))
    # Full blocks, so they need no support of their own.
    fn.cmd('setblock 29 %d 0 %s' % (Y + 2, block('simulated:velocity_sensor', facing='north')))
    fn.cmd('setblock 29 %d 0 %s' % (Y + 6, block('simulated:gimbal_sensor', axis='x')))
    # On top of the column: face=floor needs only the block beneath.
    fn.cmd('setblock 28 %d 0 %s' % (Y + 24, block('simulated:altitude_sensor',
                                                  face='floor', facing='north', dial='linear')))
    fn.blank()
    fn.sign(31, 0, 'C3 NAV+DOCK')
    fn.set(32, 0, 'simulated:navigation_table', facing='up')
    fn.set(35, 0, 'simulated:docking_connector', facing='north', extended='false', powered='false')
    fn.set(35, 5, 'simulated:docking_connector', facing='south', extended='false', powered='false')
    return fn


def module_d():
    fn = emit(Fn('module_d', 'Display wall: one Create display link per source, each',
                 'pointed at a pair of nixie tubes.'))
    fn.sign(-1, 20, 'D DISPLAYS')
    sources = ['altitude', 'gimbal', 'velocity', 'optical', 'laser',
               'engine', 'auger', 'navtable', 'typewriter']
    for i, _ in enumerate(sources):
        x = i * 3
        fn.set(x, 20, 'create:display_link')
        fn.set(x, 22, 'create:nixie_tube')
        fn.set(x + 1, 22, 'create:nixie_tube')
    return fn



def aircraft():
    """A raised airframe carrying the instruments that only read while moving.

    Altitude, velocity and gimbal sensors report nothing useful on the ground, so
    they need a craft. The deck stands on legs so the Physics Assembler has clear
    air beneath it, and the nose points east so a heading is easy to judge.
    """
    fn = emit(Fn('aircraft', 'Raised test airframe at x=60. Glue the deck, then',
                 'right-click the Physics Assembler to assemble.'))
    deck = Y + 5                      # 5 above the platform
    # 16 long, not 17: Create's super glue box reaches 16 blocks, and a join needs
    # one glue entity containing both blocks -- two boxes that merely touch do not
    # connect. At this size the whole airframe, legs included, fits a single box.
    x0, x1 = 52, 67                   # tail .. nose
    z0, z1 = -4, 4

    fn.say('Building test airframe at 60,%d,0 (nose points east).' % deck)

    # Legs, so the deck stands clear of the platform.
    for lx in (x0 + 1, x1 - 1):
        for lz in (z0 + 1, z1 - 1):
            fn.cmd('fill %d %d %d %d %d %d %s'
                   % (lx, Y, lz, lx, deck - 1, lz, block('minecraft:oak_planks')))

    # Deck.
    fn.cmd('fill %d %d %d %d %d %d %s'
           % (x0, deck, z0, x1, deck, z1, block('minecraft:oak_planks')))

    # Wings: symmetric sails attach to blocks and to each other with no glue.
    for wz in (z0 - 4, z0 - 3, z0 - 2, z0 - 1, z1 + 1, z1 + 2, z1 + 3, z1 + 4):
        for wx in range(58, 63):
            fn.cmd('setblock %d %d %d %s'
                   % (wx, deck, wz, block('simulated:white_symmetric_sail', axis='x')))

    dy = deck + 1                     # everything sits on the deck
    # Flight controls, at the nose.
    fn.cmd('setblock 65 %d 0 %s' % (dy, block('simulated:steering_wheel',
                                              facing='east', on_floor='true', waterlogged='false')))
    # The assembler's sticky face points down into the deck.
    fn.cmd('setblock 60 %d 0 %s' % (dy, block('simulated:physics_assembler',
                                              face='floor', facing='east')))
    # Power.
    fn.cmd('setblock 54 %d 0 %s' % (dy, block('simulated:white_portable_engine',
                                              facing='east', lit='false')))
    fn.cmd('setblock 53 %d 0 %s' % (dy, block('minecraft:barrel')))

    # Instruments that only mean anything in motion.
    fn.cmd('setblock 62 %d -2 %s' % (dy, block('simulated:altitude_sensor',
                                               face='floor', facing='east', dial='linear')))
    fn.cmd('setblock 63 %d -2 %s' % (dy, block('simulated:velocity_sensor', facing='east')))
    fn.cmd('setblock 64 %d -2 %s' % (dy, block('simulated:gimbal_sensor', axis='x')))
    for i, src in enumerate((62, 63, 64)):
        fn.cmd('setblock %d %d -3 %s' % (src, dy, block('create:display_link')))
        fn.cmd('setblock %d %d -4 %s' % (src, dy, block('create:nixie_tube')))

    # Crew fittings.
    fn.cmd('setblock 62 %d 2 %s' % (dy, block('simulated:white_nameplate',
                                              facing='east', position='left')))
    fn.cmd('setblock 64 %d 2 %s' % (dy, block('simulated:iron_handle', facing='up')))
    fn.cmd('setblock 58 %d 2 %s' % (dy, block('simulated:torsion_spring',
                                              facing='up', powered='false')))

    fn.sign(x0 - 1, 0, 'AIRFRAME glue deck then assemble', y=dy)
    fn.say('One Super Glue box over the whole craft: 52,%d,-4 to 67,%d,4' % (Y, dy))
    fn.say('Then right-click the Physics Assembler. Wheel is at the nose.')
    return fn

def kit():
    fn = emit(Fn('kit', 'Gives one of everything this bench needs.'))
    # Counts matter: a tool stacks to one, so asking for sixteen fills sixteen
    # slots with sixteen wrenches. Tools come as one, materials as a stack.
    ONE = [
        'create:wrench', 'create:goggles', 'create:super_glue', 'create:linked_controller',
        'simulated:honey_glue', 'simulated:plunger_launcher', 'simulated:contraption_diagram',
        'simulated:creative_physics_staff',
    ]
    MANY = [
        'create:redstone_link', 'create:display_link', 'create:nixie_tube', 'create:shaft',
        'create:cogwheel', 'create:large_cogwheel', 'create:blaze_cake',
        'minecraft:coal', 'minecraft:redstone', 'minecraft:lever', 'minecraft:comparator',
        'minecraft:honeycomb', 'minecraft:slime_ball',
        # No simulated:merging_glue -- that block is registered without an item and
        # is placed with slimeballs, so /give on it would not parse.
        'simulated:spring', 'simulated:rope_coupling', 'simulated:physics_assembler',
        'simulated:steering_wheel', 'simulated:navigation_table', 'simulated:linked_typewriter',
    ]
    for i in ONE:
        fn.cmd('give @s %s 1' % item(i))
    for i in MANY:
        fn.cmd('give @s %s 16' % item(i))
    fn.say('Kit given: one of each tool, a stack of each material.')
    fn.say('Everything else is in the Create: Simulated creative tab.')
    return fn


def build_all():
    fn = emit(Fn('build', 'Platform + every module. Stand at 0 y 0 with render distance >= 8.'))
    fn.cmd('function simtest:platform')
    for m in ('module_a', 'module_b', 'module_c', 'module_d', 'aircraft'):
        fn.cmd('function simtest:' + m)
    fn.say('Test bench built at 0,%d,0. Run "function simtest:kit" for items.' % Y)
    return fn


def clear_fn():
    """Three levels of removal.

    `clear` takes the benches, `clear_platform` takes the cobble, and
    `clear_all` takes both -- removing only the platform leaves every bench
    floating, which is what happened the first time.
    """
    fn = emit(Fn('clear', 'Removes the benches and the airframe. Platform stays.'))
    # The sensor column reaches Y+24, so the bench box has to clear Y+30.
    fn.cmd('fill -2 %d -2 40 %d 26 minecraft:air' % (Y, Y + 30))
    fn.cmd('fill 50 %d -10 70 %d 10 minecraft:air' % (Y, Y + 10))
    fn.say('Benches cleared.')

    fn2 = emit(Fn('clear_platform', 'Removes the cobble platform only.'))
    for x0 in range(-256, 256, 128):
        for z0 in range(-256, 256, 128):
            fn2.cmd('fill %d %d %d %d %d %d minecraft:air'
                    % (x0, FLOOR, z0, x0 + 127, FLOOR, z0 + 127))
    fn2.say('Platform cleared.')

    fn3 = emit(Fn('clear_all', 'Removes everything this pack builds: benches,',
                  'airframe and platform.'))
    fn3.cmd('function simtest:clear')
    fn3.cmd('function simtest:clear_platform')
    fn3.say('Everything this pack built is gone.')

    # A blunt instrument for when something has drifted outside the known boxes:
    # a 128x128 column centred on the player. One fill per layer keeps each well
    # inside the 32768-block limit.
    fn4 = emit(Fn('nuke', 'Clears a 128x128 column around you, y=195 to y=265.',
                  'Use when something has ended up outside the built area.'))
    fn4.say('Clearing 128x128 around you, y195-y265 ...')
    for y in range(195, 266):
        fn4.cmd('fill ~-64 %d ~-64 ~63 %d ~63 minecraft:air' % (y, y))
    fn4.say('Done.')
    return fn


def help_fn():
    fn = emit(Fn('help', 'What this pack provides.'))
    for line in [
        'simtest:build           platform + all modules',
        'simtest:platform        256x256 cobble at y=200',
        'simtest:platform_big    512x512 (needs render distance 16)',
        'simtest:module_a        redstone bench',
        'simtest:module_b        kinetic spine',
        'simtest:module_c        sensors',
        'simtest:module_d        display wall',
        'simtest:aircraft        raised airframe for flight tests',
        'simtest:kit             give test items',
        'simtest:clear           remove benches + airframe',
        'simtest:clear_platform  remove the cobble',
        'simtest:clear_all       remove everything above',
        'simtest:nuke            clear 128x128 around you',
    ]:
        fn.cmd('tellraw @s ' + json.dumps({'text': line, 'color': 'gray'}))
    return fn


# ---------------------------------------------------------------- write


def lint(fn):
    """Catches commands that would not parse, which reject the whole function.

    Minecraft does not run a function containing one bad command -- it discards
    the file and reports it as unknown. That is what an unescaped quote inside a
    tellraw did here: `build` and `clear` disappeared entirely while their
    siblings loaded, which reads like a missing file rather than a syntax error.
    """
    for line in fn.lines:
        if not line or line.startswith('#'):
            continue
        if line.startswith('tellraw '):
            payload = line.split(' ', 2)[2] if line.count(' ') >= 2 else ''
            try:
                json.loads(payload)
            except Exception as exc:
                PROBLEMS.append('%s: tellraw payload is not valid JSON (%s): %s'
                                % (fn.name, exc, payload[:70]))
        if line.startswith(('setblock ', 'fill ')) and line.count('[') != line.count(']'):
            PROBLEMS.append('%s: unbalanced [] in: %s' % (fn.name, line[:70]))
        if 'oak_sign' in line and line.count('{"text"') != 4:
            PROBLEMS.append('%s: sign needs exactly 4 messages: %s' % (fn.name, line[:70]))
        if line.count('{') != line.count('}'):
            PROBLEMS.append('%s: unbalanced {} in: %s' % (fn.name, line[:70]))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--install', action='store_true', help='copy into the test world')
    args = ap.parse_args()

    load_states()

    platform('platform', 128, '256x256 cobble platform. Safe at render distance 8.')
    platform('platform_big', 256, '512x512 cobble platform. Needs render distance 16.')
    module_a(); module_b(); module_c(); module_d(); aircraft()
    kit(); build_all(); clear_fn(); help_fn()

    for fn in FUNCTIONS:
        lint(fn)

    if PROBLEMS:
        print('%d problem(s) -- nothing written:' % len(PROBLEMS))
        for p in sorted(set(PROBLEMS)):
            print('  !', p)
        return 1

    if OUT.exists():
        shutil.rmtree(OUT)
    fdir = OUT / 'data/simtest/functions'
    fdir.mkdir(parents=True)
    (OUT / 'pack.mcmeta').write_text(json.dumps({
        'pack': {'pack_format': 15,
                 'description': 'Create: Simulated test bench. Run /function simtest:help'}
    }, indent=2), encoding='utf-8')
    for fn in FUNCTIONS:
        (fdir / (fn.name + '.mcfunction')).write_text('\n'.join(fn.lines) + '\n', encoding='utf-8')

    total = sum(len([l for l in f.lines if l and not l.startswith('#')]) for f in FUNCTIONS)
    print('validated %d block ids and %d item ids; wrote %d functions (%d commands)'
          % (len(_states), len(_items), len(FUNCTIONS), total))
    print('  ->', OUT)

    if args.install:
        dest = WORLD / 'datapacks' / 'simulated_testbench'
        if not WORLD.exists():
            print('world not found:', WORLD)
            return 1
        if dest.exists():
            shutil.rmtree(dest)
        shutil.copytree(OUT, dest)
        print('  installed ->', dest)
    return 0


if __name__ == '__main__':
    sys.exit(main())
