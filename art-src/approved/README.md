# Approved pond art direction

Created with the built-in `image_gen` tool, not the API/CLI fallback.

- `pond-direction.png`: user-approved complete-screen concept.
- `../../app/src/main/assets/art/pond_v2.png`: independent environment layer.
- `../../app/src/main/assets/art/angler_v2.png`: transparent character and stool layer.
- `PondArt.java` places these layers and draws the interactive rod, line, float and effects.

Only the pond environment has been replaced. The remaining five locations retain their existing art. The existing species and other third-party assets retain their bundled attribution. These two generated layers are new assets; no third-party license is asserted for them.

## Final background prompt

Produce a clean environment background layer by editing this exact game screenshot. Keep the exact 16:9 framing, dock location and dimensions, trees, house, bucket, crates, flowers, pond, colors and pixel art details. Erase all three groups of UI: top-left title, top-right icons and bottom-right button. Seamlessly fill these areas with environment. Erase fisherman, hat, body, stool, fishing rod, fishing line, bobber and its circular ripple, and both fish silhouettes. Reconstruct matching empty dock floor, hanging willow foliage or water in their places. Do not change framing, move dock, or redesign other objects. Result is an empty coherent pixel-art fishing pond background ready for a separately rendered character. No text, no UI, no human, no rod, no line, no bobber. Opaque background.

## Final character prompt

Extract and recreate ONLY the exact seated chibi fisherman and his small wooden stool from this approved game screenshot as a separate transparent PNG sprite. Same straw hat with dark band, black hair, simple face looking right, orange jacket, cream hands held out toward the right, dark blue pants and boots, sitting on the little wood stool. Exact same character identity, body proportions, pose, near-side view and chunky pixel art style as the reference. Remove the fishing rod from his hands (it will be animated separately in game); hands stay posed holding an invisible rod. No rod, fishing line, bobber, dock, bucket, background, scenery, UI or text. Only ONE complete character and stool, centered, tightly composed with approximately 8 percent transparent padding around the outer character bounding box. All else genuinely transparent alpha. Clean crisp hard pixel edges, a single coherent coarse pixel grid, limited palette 3 shades per material, deep navy outlines. Do not add detail or make it realistic or smoothly shaded. Preserve the reference character's simplified charming look. Square output.

Both edits used `pond-direction.png` as their reference. The runtime uses nearest-neighbor rendering and scans the actor alpha once on load to align the separate sprite to the dock. Generated files have independent names and are not overwritten by `tools/art/prepare_art.py`.


## Frame animation (2026-11-18)

`../../app/src/main/assets/art/angler_frames.png` is a separate transparent 4×4 sprite sheet generated with the built-in image_gen tool using `angler_v2.png` as the reference. Rows are idle/blink, casting, bite reaction, and reeling. Each contains four distinct poses. The atlas is 1254×1254; cells use fractional-grid integer boundaries, not an assumed power-of-two size. Stool anchors, baselines and hand anchors in PondArt register each frame at a fixed scale.

Runtime: idle uses a 3.2-second breathing/blink sequence with a 0.12-second closed-eye frame; casting and reeling are 0.64-second one-shots at four poses per action; bite loops at 6 fps. The float is released after the two preparation poses. A new cast interrupts the reel pose, and automatic fishing renewal does not replay casting. Both cancellation and successful catch trigger reel poses. All timelines use the pausable water animation clock. Water highlights, float bobbing, ripples and fish tails also advance in discrete frames. The five legacy locations retain their earlier character artwork.

Final generation prompt:

Create a production pixel-art ANIMATION SPRITE SHEET of this exact seated chibi fisherman. Output a square transparent PNG with EXACTLY 4 equal columns and 4 equal rows, 16 sprites, no other sprites, no text, no grid lines, no background. Each cell is exactly one quarter of total width and height. Same character throughout: wide straw hat dark band, black hair, orange jacket, cream hands, navy pants, brown boots, satchel, sitting on wooden stool looking RIGHT. Match reference's chunky clean pixel style and identity. Keep the exact same scale, camera, proportions and palette in all frames. Leave 10 percent padding in every cell. Stool bottom baseline at 88 percent of cell height, stool center x=45 percent in EVERY CELL; stool, feet and hips stationary. Change actual head/eye/arm/torso poses; never just translate an identical sprite. No fishing rod, line, fish or effects in any frame because those are separate game layers. Hands always posed to grasp an invisible fishing rod handle. Animation rows LEFT TO RIGHT: ROW 1 IDLE: relaxed eyes open hands forward; small inhale shoulders rise; eyes closed blinking with shoulders relaxed; eyes open returning original pose. ROW 2 CAST: hands close to chest preparing; both hands lifted near right cheek with torso leaned back slightly for windup; arms extend forward to right casting; settles hands forward at chest level. ROW 3 BITE: waiting eyes open hands forward; surprised eyes wide head tilts forward hands grip tighter; leans toward water hands slightly lower; pulls slightly back toward original grip. ROW 4 REEL: grips with bent elbows; elbows pull backward and hands lift to face; hands lift slightly higher excited eyes with head back; settles toward starting seated pose with happy eyes. Frame-to-frame motion must be readily visible but coherent, subtle hat/head response, stool never moves. Exactly 4x4 regular grid, transparent gutters, no cropping off hats or feet, full character and stool in each cell. Absolutely no rods. Real alpha transparency.

The main render loop is capped at 30 fps; individual sprite animations retain their authored frame timing. This leaves gaps for input and lifecycle events on slower devices.


## Stronger casting, original pixel style retained

The proposed new generated casting sheet was rejected for drifting from the approved pixel style and is NOT used or bundled. The implementation keeps `angler_frames.png` unchanged. Eight timed poses reuse its existing drawings, with nearest-neighbor upper-body squash, stretch and shear rasterized on the 320×180 world canvas. The stool, feet and hip contact stay anchored. The hand position is transformed with the body so the rod remains attached. Rod flex and recoil are keyed separately. No bitmap smoothing or new gradients are introduced.

Current timing supersedes the earlier timings: cast 0.46s (stage boundaries 0.04, 0.10, 0.18, 0.22, 0.27, 0.32, 0.38, 0.46); release at 0.18s; landing at 0.42s; reel 0.40s; bite 8fps; idle/blink cycle 2.6s. The compressed windup holds longer than the snap. The foreground water contact remains (592,548).

References consulted for key poses, anticipation, timing and recovery:
- Skullgirls / GDC, Fluid and Powerful Animation within Frame Restrictions: https://gdcvault.com/play/1020575/
- Masahiro Sakurai on Creating Games, Breaking Down Attack Animations: https://www.youtube.com/watch?v=LewXWM7HDd8

Actual game capture: `../../.screenshots/pond-cast-power.mp4`.

Verified normal-speed excerpt: `../../.screenshots/pond-cast-power-preview.mp4`. Touch-cast/reel regression also covers refreshing accessibility labels after direct touch; state changes now invalidate cached control descriptions.
