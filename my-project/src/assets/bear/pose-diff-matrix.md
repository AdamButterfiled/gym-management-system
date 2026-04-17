# Polar Bear Pose Diff Matrix

Source images:
- `/Users/adambutterfield/Desktop/polar bears/iCopy_2026_04_10_06_53_16.png`
- `/Users/adambutterfield/Desktop/polar bears/iCopy_2026_04_10_06_53_01.png`
- `/Users/adambutterfield/Desktop/polar bears/iCopy_2026_04_10_06_53_12.png`
- `/Users/adambutterfield/Desktop/polar bears/iCopy_2026_04_10_06_53_07.png`
- `/Users/adambutterfield/Downloads/polar_bear_vector_detailed_v3_noborder.svg`

Generated research assets:
- `research/hello-wave.svg`
- `research/cover-peek.svg`
- `research/error-sad.svg`
- `research/success-lift.svg`

## Reuse / Replace Matrix

| Group | hello-wave | cover-peek | error-sad | success-lift | Notes |
| --- | --- | --- | --- | --- | --- |
| head-shell | reuse | reuse | reuse with slight tilt | reuse | Head silhouette stays stable. |
| ears | reuse | reuse | reuse | reuse | Ear size and placement remain fixed. |
| brows | happy | default | sad | happy | Brows swap by state, not by morphing the whole head. |
| eyes / pupils / lids | open + idle blink | covered, peek only | open but fixed | open, slightly lifted | Only idle tracks the pointer. |
| muzzle / nose | reuse | reuse | reuse | reuse | Nose and muzzle proportions stay stable. |
| mouth | wide open | soft smile | downturned curve | widest open | Mouth is a dedicated per-state path. |
| torso | reuse | reuse | reuse with subtle compression | reuse | Torso fur language stays consistent. |
| armL / pawL | replace with wave arm | replace with cover arm | replace with resting sad arm | replace with lift arm | Left arm carries most of the expressive motion. |
| armR / pawR | replace with run-back arm | replace with cover arm | replace with resting sad arm | replace with lift arm | Right arm mirrors state intent rather than morphing continuously. |
| legL | replace with run stride | reuse stand leg | reuse stand leg | replace with lifted leg | Left leg is the main motion leg. |
| legR | replace with run stride | reuse stand leg | reuse stand leg | replace with support leg | Right leg anchors the pose in most states. |
| shadow | replace with run ellipse | reuse | reuse | replace with bounce ellipse | Shadow follows pose and weight shift. |
| props | scooter | none | none | dual dumbbells | Props are state-only and do not exist in idle. |

## Runtime Mapping

- `intro`: common body + happy face + run legs + wave arm + scooter
- `idle`: common body + neutral happy face + stand legs + rest arms
- `password`: common body + neutral face + stand legs + dedicated cover arms + timed peek offset
- `error`: common body + sad brows + sad mouth + stand legs + lowered arms + head tilt
- `success`: common body + happy face + lift legs + lift arms + dual dumbbells
