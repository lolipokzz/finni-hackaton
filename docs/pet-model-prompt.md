# Промпт для генерации 3D-модели питомца

Приложить к запросу: скриншот текущего кролика (главный экран) и файл `app/src/main/assets/pet.glb`.
Промпт на английском: генераторы 3D (Meshy, Tripo, Hunyuan3D, Rodin) и мультимодальные LLM понимают его точнее.
Русская версия ниже — для LLM, которая пишет техзадание художнику.

---

## English (for 3D generators and multimodal LLMs)

You are given a reference: a stylized 3D bunny character (attached GLB + screenshot). Recreate this exact character
with higher quality, in the visual style of "My Talking Tom" (Outfit7): a glossy, soft, rounded 3D cartoon mascot for
a children's app (ages 7–11).

Keep the character's identity:
- a small humanoid bunny in a coral-pink hooded onesie, long upright rabbit ears on the hood, a tuft of three spiky
  "hair" locks between the ears, a round cream-colored face with two large oval dark-navy eyes, a light mint belly
  patch, cream paws and feet;
- friendly, curious, slightly shy expression; no teeth, no sharp shapes.

Style upgrade (My Talking Tom look):
- smooth subdivision-quality surfaces, soft rounded silhouette, subtle plush/velvet feel on the onesie;
- big glossy eyes with specular highlights and slight eyelid shape, tiny nose, soft cheek blush;
- clean PBR materials, no baked shadows, no outlines; colors: coral #FF6F61 (onesie), cream #F5DEB3 (face/paws),
  mint #BFE3DA (belly), navy #1E2545 (eyes);
- proportions: big head (about 45% of total height), short body, stubby arms and legs, standing pose, facing camera.

Deliver THREE growth stages of the same character, identical style and colors, as three separate models:
1. BABY: chubby, head ≈ 50% of height, ears shorter and floppy at the tips, sitting-or-standing with feet apart,
   round belly, large sparkly eyes.
2. TEEN: slimmer, head ≈ 42% of height, ears straight and tall, upright confident stance, small bandana or
   scarf accent in mint.
3. ADULT: tallest, head ≈ 38% of height, ears longest, relaxed stance with one hand on hip, small backpack or
   satchel accent in navy.
Overall heights should read as small / medium / large when placed side by side (about 1.0 : 1.25 : 1.5).

Technical requirements (must match the existing app pipeline):
- format: glTF 2.0 binary (.glb), one file per stage: pet_baby.glb, pet_teen.glb, pet_adult.glb;
- mobile-friendly: 8k–20k triangles per model, no textures larger than 1024 px (untextured PBR vertex/material
  colors are preferred);
- the main onesie material must be named exactly "Main" (the app tints it programmatically to the pet's color);
  keep face/paws material as "Main_Light", belly as "Main2", eyes as "EyeColor";
- rigged with a simple humanoid skeleton (root, hips, spine, neck, head, ears, arms with hands, legs with feet);
- animations embedded in the GLB, named exactly: "Idle" (gentle breathing/bobbing loop, 2–3 s, seamless) and
  "Wave" (right-hand wave, 1.5–2 s, starts and ends in the Idle pose). Optional extras: "Happy" (small jump),
  "Sad" (slouch, ears droop), "Eat";
- Y-up, character facing +Z, feet on the ground plane at y = 0, real-world scale ≈ 1 m tall for TEEN;
- single scene root node named "CharacterArmature", no lights, no cameras, no extra empties;
- test render each stage on a plain light-lavender background, front view, soft top-front light.

Also produce a character sheet image (PNG, 2048×1024): the three stages side by side, front view, same scale,
labeled BABY / TEEN / ADULT, plus a small expression strip for the TEEN (happy, calm, sad).

---

## Русская версия (для LLM, которая пишет ТЗ художнику или описывает модель)

Дан референс: стилизованный 3D-кролик (приложены GLB и скриншот). Нужно воссоздать этого же персонажа в более
высоком качестве, в стиле «Мой Том» (Outfit7): глянцевый, мягкий, округлый 3D-маскот для детского приложения 7–11 лет.

Сохранить узнаваемость: маленький кролик-человечек в коралловом комбинезоне с капюшоном, длинные стоячие уши на
капюшоне, три задорных пряди между ушами, круглое кремовое лицо, два больших овальных тёмно-синих глаза, мятное
пятно на животе, кремовые лапы. Выражение дружелюбное, любопытное, чуть застенчивое. Никаких зубов и острых форм.

Три стадии роста одного персонажа, одинаковый стиль и цвета, три отдельные модели:
1. МАЛЫШ: пухлый, голова ≈ 50 % роста, уши короче и с мягкими кончиками, круглый живот, огромные блестящие глаза.
2. ПОДРОСТОК: стройнее, голова ≈ 42 %, уши прямые и высокие, уверенная стойка, мятный шарфик или бандана.
3. ВЗРОСЛЫЙ: самый высокий, голова ≈ 38 %, уши самые длинные, расслабленная поза с рукой на боку, маленький
   тёмно-синий рюкзачок. Рост стадий соотносится примерно как 1.0 : 1.25 : 1.5.

Технические требования те же, что в английской версии: glTF 2.0 (.glb) по файлу на стадию, 8–20 тыс. треугольников,
материал комбинезона строго с именем "Main" (приложение перекрашивает его программно), скелет, встроенные анимации
"Idle" и "Wave" с точными именами, Y вверх, лицом к +Z, ноги на y = 0, один корневой узел "CharacterArmature".
Дополнительно: лист персонажа PNG 2048×1024 с тремя стадиями рядом и полоской эмоций для подростка.
