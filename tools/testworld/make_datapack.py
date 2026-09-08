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
    # The lang file is the authoritative list of what is registered. Item *models*
    # are not: upstream ships only one of the sixteen symmetric sail item models,
    # and none for merging glue, so keying off models reports registered items as
    # missing.
    lang = json.loads((SIM_BS.parent / 'lang/en_us.json').read_text(encoding='utf-8'))
    for k in lang:
        if k.startswith('item.simulated.') or k.startswith('block.simulated.'):
            name = k.split('.', 2)[2]
            if '.' not in name:
                _items.add('simulated:' + name)

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
        for name in z.namelist():
            if name.startswith('assets/create/blockstates/') and name.endswith('.json'):
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
              'minecraft:repeater', 'minecraft:barrel', 'minecraft:hopper'}


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
        return self.cmd('tellraw @s {"text":"%s","color":"aqua"}' % msg)

    def set(self, x, z, bid, y=Y, **props):
        return self.cmd('setblock %d %d %d %s' % (x, y, z, block(bid, **props)))

    def sign(self, x, z, text, y=Y):
        # A short label so each bench is identifiable in world.
        lines = [text[i:i + 15] for i in range(0, min(len(text), 60), 15)]
        msgs = ','.join('"\\"%s\\""' % l for l in lines[:4])
        return self.cmd('setblock %d %d %d minecraft:oak_sign{front_text:{messages:[%s]}}'
                        % (x, y, z, msgs if msgs else '"\\"\\""'))


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


def kit():
    fn = emit(Fn('kit', 'Gives one of everything this bench needs.'))
    items = [
        'create:wrench', 'create:goggles', 'create:super_glue', 'create:redstone_link',
        'create:display_link', 'create:nixie_tube', 'create:shaft', 'create:cogwheel',
        'create:large_cogwheel', 'create:linked_controller', 'create:blaze_cake',
        'minecraft:coal', 'minecraft:redstone', 'minecraft:lever', 'minecraft:comparator',
        'minecraft:honeycomb',
    ]
    sim = ['simulated:honey_glue', 'simulated:merging_glue', 'simulated:spring',
           'simulated:rope_coupling', 'simulated:plunger_launcher', 'simulated:contraption_diagram',
           'simulated:creative_physics_staff', 'simulated:physics_assembler',
           'simulated:steering_wheel', 'simulated:navigation_table', 'simulated:linked_typewriter']
    for i in items + sim:
        fn.cmd('give @s %s 16' % item(i))
    fn.say('Kit given. Simulated blocks are all in the Create: Simulated creative tab.')
    return fn


def build_all():
    fn = emit(Fn('build', 'Platform + every module. Stand at 0 y 0 with render distance >= 8.'))
    fn.cmd('function simtest:platform')
    for m in ('module_a', 'module_b', 'module_c', 'module_d'):
        fn.cmd('function simtest:' + m)
    fn.say('Test bench built at 0,%d,0. Run "function simtest:kit" for items.' % Y)
    return fn


def clear_fn():
    fn = emit(Fn('clear', 'Removes everything this pack placed. Platform stays.'))
    fn.cmd('fill -2 %d -2 40 %d 26 minecraft:air' % (Y, Y + 30))
    fn.say('Bench cleared. "function simtest:clear_platform" removes the cobble too.')
    fn2 = emit(Fn('clear_platform', 'Removes the cobble platform.'))
    for x0 in range(-256, 256, 128):
        for z0 in range(-256, 256, 128):
            fn2.cmd('fill %d %d %d %d %d %d minecraft:air'
                    % (x0, FLOOR, z0, x0 + 127, FLOOR, z0 + 127))
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
        'simtest:kit             give test items',
        'simtest:clear           remove the bench',
    ]:
        fn.cmd('tellraw @s {"text":"%s","color":"gray"}' % line)
    return fn


# ---------------------------------------------------------------- write

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--install', action='store_true', help='copy into the test world')
    args = ap.parse_args()

    load_states()

    platform('platform', 128, '256x256 cobble platform. Safe at render distance 8.')
    platform('platform_big', 256, '512x512 cobble platform. Needs render distance 16.')
    module_a(); module_b(); module_c(); module_d()
    kit(); build_all(); clear_fn(); help_fn()

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
