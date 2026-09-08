/**
 * A stand-in for Sable, the physics engine, mirroring its package layout.
 *
 * <h2>Why this exists</h2>
 *
 * 121 of upstream's 584 files import Sable, and Sable does not build on 1.20.1
 * yet — bringing it up is the whole of V2. Everything in V1 that touches physics
 * touches it at the edges: a block asks "am I inside a sub-level?", and does one
 * thing if it is and another if it is not. On a world-placed block the answer is
 * always no, and that is exactly the case V1 ships.
 *
 * <p>So this package answers "no sub-level" to every query, and is otherwise
 * real code: the maths types ({@code JOMLConversion}, {@code Pose3d},
 * {@code BoundingBox3d}) are proper implementations, because they are pure
 * geometry with no engine behind them.
 *
 * <h2>What that means for behaviour</h2>
 *
 * A ported file keeps upstream's control flow unchanged and takes its
 * no-sub-level branch. Nothing here fabricates a physics result: a sub-level
 * lookup returns {@code null}, a force queue accepts and discards, a constraint
 * is never created. Where upstream's only behaviour is the physics one, the
 * feature is inert — recorded per feature in FABRIC_PORT_PLAN.md rather than
 * left to be discovered.
 *
 * <h2>How V2 removes it</h2>
 *
 * The layout below {@code backport.physics} mirrors {@code dev.ryanhcode.sable}
 * one-for-one, and {@code tools/port_upstream.py} rewrites the import prefix on
 * the way in. When Sable builds, that rewrite is dropped from the tool, the
 * ported files are re-imported against the real engine, and this package is
 * deleted. Nothing outside it needs to change.
 */
package dev.simulated_team.simulated.backport.physics;
