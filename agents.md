# WARGAME.IO – Technická špecifikácia a implementačná príručka pre natívny Android (Pure Kotlin + Canvas)

Tento dokument slúži ako **kompletná, vyčerpávajúca a deterministická špecifikácia** hry WARGAME.IO pre vývojárov, ktorí chcú vytvoriť **100% natívnu Android verziu v čistom Kotline** bez použitia akéhokoľvek WebView, Reactu, Cordova/Capacitoru či ťažkých herných enginov (Unity/Unreal). Hra je navrhnutá tak, aby bežala cez natívne Android 2D `Canvas` API (`SurfaceView` alebo `TextureView`) pri 60/120 FPS.

---

## 1. Koncept a herný princíp (Gameplay Mechanics)

- **Žáner:** Real-time taktická akčná stratégia (Node-conquest / Swarm IO).
- **Cieľ hráča:** Dobyť všetky nepriateľské základne na mape vysielaním rojov jednotiek zo svojich základní a elimináciou protivníkov.
- **Základný priebeh:**
  1. Hráč začína s jednou alebo viacerými základňami (Farba: **Cyan #00D2FF**).
  2. Na mape sú neutrálne základne (Sivá #64748B) a 1 až 5 nepriateľských frakcií (Červená, Zelená, Oranžová, Fialová, Žltá).
  3. Základne, ktoré niekto vlastní (majiteľ != 0), autonómne generujú vojsko v pravidelných intervaloch až do svojej maximálnej kapacity.
  4. Hráč potiahnutím prsta (Swipe/Drag) vyberie jednu alebo viac svojich základní a namieri na cieľovú základňu.
  5. Základne začnú sekvenčne chrliť jednotky v intervale 50 ms.
  6. Jednotky letia k cieľu pomocou vektorového fyzikálneho systému **Steering Behaviors** (vyhýbajú sa cudzím základniam a prekážkam na bojisku).
  7. **Zrážka na bojisku:** Ak sa v poli zrazia nepriateľské jednotky, dochádza k vzájomnej 1vs1 eliminácii (90% šanca na okamžité zničenie oboch, vizuálne iskry).
  8. **Útok na základňu:** Každá nepriateľská jednotka, ktorá vstúpi do obvodu základne, zníži jej posádku o 1. Ak stav klesne na 0, základňa je dobytá útočníkom s 1 jednotkou a rázovou vlnou.
  9. **Posilnenie vlastnej základne:** Ak do nej dorazí vlastná jednotka, navýši jej stav o 1.
  10. **Podmienka víťazstva:** Všetky nepriateľské základne aj nepriateľské jednotky v poli sú zničené (`enemyBases == 0 && enemyUnits == 0 && playerBases > 0`).
  11. **Podmienka prehry:** Hráč stratil všetky základne a nemá žiadne jednotky v poli (`playerBases == 0 && playerUnits == 0`).

---

## 2. Architektúra natívnej Android aplikácie

```
app/src/main/java/cdev/wargame/
│
├── core/
│   ├── Vector2.kt                 // Matematický 2D vektor (add, sub, mul, normalize, dot, dist)
│   ├── GameConfig.kt              // Herné konštanty, rozmery, farby frakcií, steering váhy
│   ├── GameEngine.kt              // Hlavný riadiaci stav hry, update cyklus, pravidlá
│   ├── SteeringBehavior.kt        // Algoritmus letu jednotiek (seek, separation, repel, tangent flow)
│   └── CampaignManager.kt         // Perzistencia (SharedPreferences), odomykanie levelov, rank
│
├── entities/
│   ├── Entity.kt                  // Abstraktná základná trieda (id, position, radius, ownerId)
│   ├── Base.kt                    // Základňa (generovanie jednotiek, kapacita, spawner)
│   ├── Unit.kt                    // Jednotka vojska (pozícia, rýchlosť, smer, kolízie)
│   └── Obstacle.kt                // Prekážka (asteroid, skala, mnohouholník, rotácia)
│
├── physics/
│   ├── CollisionSystem.kt         // Detekcia kolízií (jednotka-jednotka, jednotka-prekážka, arrival)
│   └── SpatialGrid.kt             // Voliteľná mriežka pre O(N) optimalizáciu pri tisícoch jednotiek
│
├── effects/
│   ├── Particle.kt                // Iskry, kinetické čiastočky pri zrážkach
│   ├── Ripple.kt                  // Rázové vlny pri dobytí základne
│   └── ParticleSystem.kt          // Manažér a vykresľovač vizuálnych efektov
│
├── ai/
│   └── AiAgent.kt                 // Autonómna umelá inteligencia pre každého nepriateľa
│
├── levels/
│   ├── LevelData.kt               // Dátové modely pre JSON definície levelov
│   └── LevelLoader.kt             // Načítanie a normalizácia levelov z assets/levels/*.json
│
├── audio/
│   └── SoundManager.kt            // SoundPool audio efekty (alebo AudioTrack syntéza)
│
└── ui/
    ├── GameSurfaceView.kt         // Natívny herný View (SurfaceHolder.Callback, render thread)
    ├── CanvasRenderer.kt          // Vykresľovanie na android.graphics.Canvas
    ├── GameInputHandler.kt        // OnTouchListener (multi-drag, detekcia základní, hit-boxy)
    └── GameActivity.kt            // Hlavná aktivita (fullscreen, lifecycle, pause)
```

---

## 3. Matematický model a súradnicový systém

### 3.1 Normalizovaný logický priestor (Logical Coordinates)
Herný svet nie je viazaný na pixely, ale na normalizovaný priestor so stredom v `(0, 0)`:
- Na displeji na výšku (**Portrait**, `width < height`):
  $$\text{aspect} = \frac{\text{width}}{\text{height}}$$
  $$X \in [-1.0, 1.0] \quad (\text{šírka } = 2.0)$$
  $$Y \in \left[-\frac{1}{\text{aspect}}, \frac{1}{\text{aspect}}\right]$$
- Na displeji na šírku (**Landscape**, `width \ge height`):
  $$X \in [-\text{aspect}, \text{aspect}], \quad Y \in [-1.0, 1.0]$$

### 3.2 Prepočet Logických súradníc na Pixely obrazovky
V triede `CanvasRenderer`:
$$\text{scale} = \begin{cases} \frac{\text{width}}{2.0} & \text{ak Portrait} \\ \frac{\text{height}}{2.0} & \text{ak Landscape} \end{cases}$$
$$\text{centerX} = \frac{\text{width}}{2.0}, \quad \text{centerY} = \frac{\text{height}}{2.0}$$
$$\text{toScreenX}(x) = \text{centerX} + x \cdot \text{scale}$$
$$\text{toScreenY}(y) = \text{centerY} + y \cdot \text{scale}$$
$$\text{toScreenRadius}(r) = r \cdot \text{scale}$$

Inverzný prepočet (z dotyku prsta na logické súradnice):
$$\text{toLogicalX}(\text{touchX}) = \frac{\text{touchX} - \text{centerX}}{\text{scale}}$$
$$\text{toLogicalY}(\text{touchY}) = \frac{\text{touchY} - \text{centerY}}{\text{scale}}$$

### 3.3 UI Proporcionálna mierka (`s`)
Pre konzistentný vzhľad čiar, textov a prstencov na rôznych hustotách displejov:
$$s = \frac{\text{width}}{350.0}$$
Všetky hrúbky čiar a písma sa násobia faktorom `s`.

---

## 4. Konfigurácia, konštanty a frakcie (`GameConfig`)

### 4.1 Frakcie a farby
| ID | Názov | Primary Hex | Glow / Light | Dark Hex |
|---|---|---|---|---|
| **0** | Neutrál | `#64748B` | `#94A3B8` | `#334155` |
| **1** | Hráč (Cyan) | `#00D2FF` | `#38BDF8` | `#0284C7` |
| **2** | Nepriateľ Červený | `#FF3366` | `#FB7185` | `#E11D48` |
| **3** | Nepriateľ Zelený | `#10B981` | `#34D399` | `#059669` |
| **4** | Nepriateľ Oranžový | `#F59E0B` | `#FBBF24` | `#D97706` |
| **5** | Nepriateľ Fialový | `#A855F7` | `#C084FC` | `#7E22CE` |
| **6** | Nepriateľ Žltý | `#EAB308` | `#FDE047` | `#CA8A04` |

### 4.2 Typy a vlastnosti základní
| Typ (`BaseType`) | Max jednotiek | Logický polomer ($r$) | Rýchlosť obnovy (`regenRateMs`) |
|---|---|---|---|
| `small` | 25 | 0.08 | 1200 ms (1 jednotka / 1.2s) |
| `medium` | 50 | 0.10 | 800 ms (1 jednotka / 0.8s) |
| `large` | 100 | 0.13 | 500 ms (1 jednotka / 0.5s) |

### 4.3 Jednotky vojska
- **Logický polomer jednotky:** $r_{\text{unit}} = 0.02$
- **Základná rýchlosť:** $v_{\text{base}} = 0.35$ logických jednotiek za sekundu.
- **Variabilita rýchlosti:** $\pm 15\%$ pre prirodzený pohyb roja:
  $$v = v_{\text{base}} \cdot (1 + (\text{random}(-1, 1) \cdot 0.15))$$
- **Interval spawnovania:** 50 ms medzi jednotlivými jednotkami vysielanými zo základne.
- **Šanca vzájomného zničenia v boji:** 90% (`0.90`).

### 4.4 Váhy Steering Behavior
- `SEEK_WEIGHT = 5.0` (Smerovanie k cieľovej základni)
- `SEPARATION_WEIGHT = 2.5` (Rozostup medzi vlastnými jednotkami v roji)
- `SEPARATION_RADIUS = 1.15 * r_unit`
- `BOUNDARY_MARGIN = 0.08` (Odtlak od okraja hracej plochy)
- `BOUNDARY_FORCE = 3.0`
- `BASE_AVOIDANCE_RADIUS = 0.25` (Odpudzovacia zóna okolo cudzích základní)
- `BASE_REPEL_WEIGHT = 9.0` (Radiálna sila od stredu základne)
- `BASE_TANGENT_WEIGHT = 1.5` (Tangenciálne obtekanie okolo základne)
- `OBSTACLE_AVOIDANCE_RADIUS = 0.25`
- `OBSTACLE_REPEL_WEIGHT = 11.0`
- `OBSTACLE_TANGENT_WEIGHT = 2.2`

---

## 5. Fyzika pohybu a Steering Behaviors (`SteeringBehavior.kt`)

Jednotky sa nepohybujú tupo po priamke, ale využívajú vektorové sily s **garanciou presne konštantnej skalárnej rýchlosti** (jednotka nespomaľuje ani nezrýchľuje, mení sa len smer vektora rýchlosti $\vec{v}$).

### 5.1 Algoritmus výpočtu smeru:
1. **Smer k cieľu:**
   $$\vec{d}_{\text{target}} = \text{normalize}(\vec{P}_{\text{target}} - \vec{P}_{\text{unit}})$$
2. **Separácia v roji:**
   Pre každú ďalšiu živú jednotku rovnakého majiteľa vo vzdialenosti $d < \text{SEPARATION\_RADIUS}$:
   $$\vec{F}_{\text{sep}} += \text{normalize}(\vec{P}_{\text{unit}} - \vec{P}_{\text{other}}) \cdot \left(1 - \frac{d}{\text{SEPARATION\_RADIUS}}\right)$$
3. **Odpudzovanie a obtekanie cudzích základní:**
   Pre každú základňu okrem cieľovej (a okrem východzej, kým ju jednotka neopustí):
   Vzdialenosť od stredu základne $d = |\vec{P}_{\text{base}} - \vec{P}_{\text{unit}}|$.
   Ak $d < (\text{base.radius} + \text{unit.radius} + \text{BASE\_AVOIDANCE\_RADIUS})$:
   - Pomer blízkosti: $\text{ratio} = \frac{\text{avoidDist} - d}{\text{avoidDist} - \text{minDist}}$, $\text{intensity} = \text{ratio}^2$.
   - **Radiálny odtlak:** $\vec{F}_{\text{radial}} = \text{normalize}(\vec{P}_{\text{unit}} - \vec{P}_{\text{base}}) \cdot (\text{intensity} \cdot 9.0)$
   - **Tangenciálne obtekanie (2D Cross Product):**
     $$\text{cross} = v_x \cdot \Delta y - v_y \cdot \Delta x$$
     $$\vec{T} = \begin{cases} (-\text{fromBase}_y, \text{fromBase}_x) & \text{ak cross} \ge 0 \\ (\text{fromBase}_y, -\text{fromBase}_x) & \text{ak cross} < 0 \end{cases}$$
     $$\vec{F}_{\text{tangent}} = \vec{T} \cdot (\text{intensity} \cdot 1.5)$$
4. **Odpudzovanie a obtekanie prekážok:**
   Rovnaký princíp ako u základní, s váhami $11.0$ (radiál) a $2.2$ (tangenta).
5. **Výsledný žiadaný vektor:**
   $$\vec{D}_{\text{desired}} = \vec{d}_{\text{target}} \cdot 5.0 + \vec{F}_{\text{sep}} \cdot 2.5 + \vec{F}_{\text{boundary}} + \vec{F}_{\text{baseRepel}} + \vec{F}_{\text{obsRepel}}$$
6. **Aplikácia konštantnej rýchlosti a vyhladenie:**
   $$\vec{V}_{\text{desired}} = \text{normalize}(\vec{D}_{\text{desired}}) \cdot \text{unit.speed}$$
   $$\vec{\Delta V} = (\vec{V}_{\text{desired}} - \vec{v}) \cdot \min(1.0, dt \cdot 16.0)$$
   $$\vec{v} = \text{normalize}(\vec{v} + \vec{\Delta V}) \cdot \text{unit.speed}$$
7. **Tvrdá bariéra (Position Correction):**
   Ak by aj napriek steer force jednotka prenikla do cudzej základne alebo prekážky ($d < r_{\text{obj}} + r_{\text{unit}}$):
   - Pozícia sa okamžite vysunie na obvod: $\vec{P}_{\text{unit}} = \vec{P}_{\text{obj}} + \text{normalize}(\vec{P}_{\text{unit}} - \vec{P}_{\text{obj}}) \cdot (r_{\text{obj}} + r_{\text{unit}})$.
   - Radiálna zložka rýchlosti dovnútra telesa sa vynuluje.

---

## 6. Systém dotykového ovládania (Touch Drag Interaction)

Ovládanie podporuje **multi-výber potiahnutím jedným prstom**:

1. **`ACTION_DOWN`:**
   - Prepočítať dotyk `(x, y)` na logické súradnice `(lx, ly)`.
   - Zistiť, či bol zasiahnutý kruh základne hráča (`ownerId == 1`).
   - *Tolerancia dotyku na mobile:* Použiť zväčšený hit-test rádius:
     $$\text{hitRadius} = \max(\text{base.radius}, \frac{42 \text{ dp}}{\text{scale}})$$
   - Ak je základňa hráčova:
     - `dragState.active = true`
     - `dragState.startBases = [base]`
     - Prehrať zvuk výberu (`playDragSelect()`) a aktivovať haptickú odozvu.
2. **`ACTION_MOVE`:**
   - Aktualizovať `dragState.pointerX` a `dragState.pointerY`.
   - Hit-test na základne pod prstom:
     - Ak prst prejde ponad ďalšiu **vlastnú základňu** (`ownerId == 1`), ktorá ešte nie je v zozname `startBases`, automaticky sa doň pridá! (Hráč jedným ťahom spojí napríklad 3 svoje základne).
     - Aktualizuje sa `dragState.hoverBase` (cieľová základňa, na ktorú prst práve ukazuje).
3. **`ACTION_UP` / `ACTION_CANCEL`:**
   - Ak je `dragState.hoverBase != null`:
     - Pre každú základňu zo zoznamu `startBases` (ktorá nie je identická s cieľom a má aspoň 1 jednotku):
       $$\text{startBase.sendUnitsTo}(\text{hoverBase}, \text{ratio} = 1.0)$$
     - Prehrať zvuk vyslania vojsk (`playUnitsDispatched()`).
   - Vynulovať `dragState.active = false`, vyprázdniť `startBases`.

---

## 7. Umelá inteligencia nepriateľov (`AiAgent.kt`)

Pre každého nepriateľa na mape (každé unikátne `ownerId > 1`) beží nezávislá inštancia `AiAgent`:

- **Interval rozhodovania:** Každých 2.0 až 3.0 sekundy (s náhodným fázovým posunom, aby neútočili všetci naraz).
- **Logika rozhodovania:**
  1. Získať zoznam svojich základní (`ownerId == this.ownerId`). Ak je zoznam prázdny, AI je mŕtva.
  2. Získať zoznam cudzích základní (`ownerId != this.ownerId`).
  3. Zoradiť vlastné základne zostupne podľa počtu vojska a vybrať najsilnejšiu.
  4. AI vykoná útok len vtedy, ak najsilnejšia základňa má **aspoň 8 jednotiek** (`unitCount >= 8`).
  5. **Taktický výber cieľa (Vulnerability Scoring):**
     Každá cudzia základňa dostane skóre:
     $$\text{Score} = \text{base.unitCount} \cdot 1.5 + \text{distanceToTarget} \cdot 20.0$$
     *(Prednosť majú slabé, neutrálne základne v tesnej blízkosti).*
  6. Základňa s najnižším skóre sa stane cieľom:
     $$\text{strongestBase.sendUnitsTo}(\text{targetBase}, \text{ratio} = 0.5)$$
     *(AI posiela 50% svojej armády, aby si ponechala obranu).*

---

## 8. Vizuálny štýl a vykresľovanie (`CanvasRenderer.kt`)

Vykresľovanie prebieha vo vrstvách priamo na `android.graphics.Canvas`:

```
[Vrstvy vykresľovania zozadu dopredu]
1. Pozadie: Tmavá taktická modročierna (#090D16)
2. Taktická mriežka: Tenké čiary (gridSize = 35 * s) s farbou rgba(255, 255, 255, 0.025)
3. Laserové vodiace čiary pri ťahaní prsta (Drag Tethers) s animovaným DashPathEffect
4. Rázové vlny (Ripples) pri dobytí základní (rozpínajúce sa kružnice s miznúcou alfou)
5. Prekážky (Asteroidy/skaly): Mnohoúholníky s radiálnym gradientom, hranami a fazetami
6. Základne (Bases): Ploché plné kruhy, prstenec kapacity, dynamické číslo stavu
7. Jednotky vojska (Units): Kinetické kvapky/šípky natočené v smere pohybu s bielym hrotom
8. Časticový systém (Particles): Iskry pri zrážkach
9. Horný HUD: Segmentovaný ukazovateľ prevahy síl (Power Bar) + Tlačidlá Pause / Reset
```

### 8.1 Detail vykreslenia základne (Base):
- **Telo základne:** Plný kruh vo farbe frakcie (`Paint.Style.FILL`).
- **Prstenec kapacity (Capacity Ring):**
  - Podkladový krúžok: `Color.argb(38, 255, 255, 255)`, hrúbka $2.2 \cdot s$.
  - Aktuálna kapacita: `canvas.drawArc()` od uhla `-90°` s rozsahom $\frac{\text{unitCount}}{\text{maxUnits}} \cdot 360^{\circ}$. Farba: Svetlá farba frakcie, alebo zlatá `#FFC629` pri 100% naplnení.
- **Výberový prstenec (pri ťahaní):** Biely pulzujúci kruh s polomerom $r + 7 \cdot s$.
- **Číslo posádky v strede:** Bold typografia, vycentrovaná vertikálne aj horizontálne, s jemným tieňom pre kontrast na akomkoľvek pozadí.

### 8.2 Detail vykreslenia jednotky (Unit):
- **Orientácia:** Uhol rotácie $\theta = \text{atan2}(v_y, v_x)$.
- **Telo:** Malý kruh vo farbe frakcie + trojuholníkový hrot natiahnutý v smere rotácie.
- **Glow efekt:** `paint.setShadowLayer()` vo farbe žiarenia danej frakcie.

### 8.3 Laserové lúče (Drag Tethers):
- Medzi každou vybranou základňou a prstom/cieľom sa vykreslí čiara.
- `Paint.setPathEffect(DashPathEffect(floatArrayOf(12f * s, 8f * s), phase))`, kde fáza sa posúva s časom (`phase = -(now / 20) % (20 * s)`), čo vytvára efekt "tečúcej laserovej energie".
- Na konci pri cieli je vykreslený smerový trojuholník (šípka).

---

## 9. Štruktúra levelov a JSON formát (`assets/levels/`)

Každý level je definovaný jednoduchým a čistým JSON súborom uloženým v `app/src/main/assets/levels/levelX.json`:

```json
{
  "bases": [
    { "id": "b1", "x": -0.8, "y": 0.8, "ownerId": 1, "initialUnits": 20, "type": "large" },
    { "id": "b2", "x": 0.8, "y": -0.8, "ownerId": 2, "initialUnits": 20, "type": "large" },
    { "id": "b3", "x": 0.0, "y": 0.0, "ownerId": 0, "initialUnits": 10, "type": "medium" },
    { "id": "b4", "x": -0.5, "y": -0.5, "ownerId": 0, "initialUnits": 5, "type": "small" },
    { "id": "b5", "x": 0.5, "y": 0.5, "ownerId": 0, "initialUnits": 5, "type": "small" }
  ],
  "obstacles": [
    { "id": "obs1", "x": -0.35, "y": 0.35, "radius": 0.075, "type": "rock" },
    { "id": "obs2", "x": 0.35, "y": -0.35, "radius": 0.075, "type": "rock" }
  ]
}
```

---

## 10. Hotové Kotlin implementácie kľúčových komponentov

Nasledujúce bloky kódu sú pripravené na priame skopírovanie do natívneho Android projektu:

### 10.1 `Vector2.kt`
```kotlin
package cdev.wargame.core

import kotlin.math.sqrt

data class Vector2(var x: Float = 0f, var y: Float = 0f) {
    fun add(v: Vector2): Vector2 = Vector2(x + v.x, y + v.y)
    fun sub(v: Vector2): Vector2 = Vector2(x - v.x, y - v.y)
    fun mul(scalar: Float): Vector2 = Vector2(x * scalar, y * scalar)
    fun div(scalar: Float): Vector2 = if (scalar != 0f) Vector2(x / scalar, y / scalar) else Vector2(0f, 0f)
    
    fun magSq(): Float = x * x + y * y
    fun mag(): Float = sqrt(magSq())
    
    fun normalize(): Vector2 {
        val m = mag()
        return if (m > 0f) div(m) else Vector2(0f, 0f)
    }
    
    fun distanceTo(v: Vector2): Float = sub(v).mag()
    fun dot(v: Vector2): Float = x * v.x + y * v.y
    fun copy(): Vector2 = Vector2(x, y)
}
```

### 10.2 `Base.kt`
```kotlin
package cdev.wargame.entities

import cdev.wargame.core.GameConfig
import cdev.wargame.core.Vector2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class BaseType(val maxUnits: Int, val radius: Float, val regenRateMs: Long) {
    SMALL(25, 0.08f, 1200L),
    MEDIUM(50, 0.10f, 800L),
    LARGE(100, 0.13f, 500L)
}

class Base(
    val id: String,
    var position: Vector2,
    var ownerId: Int,
    var unitCount: Int,
    val type: BaseType = BaseType.MEDIUM
) {
    val radius: Float = type.radius
    val maxUnits: Int = type.maxUnits
    
    private var regenTimer: Float = 0f
    private var spawnTimer: Float = 0f
    
    var targetBase: Base? = null
    var sendingUnitsCount: Int = 0

    fun update(dtMs: Float, onSpawnUnit: (Unit) -> Unit) {
        // Regenerácia jednotiek (len pre vlastnené základne)
        if (ownerId != 0) {
            regenTimer += dtMs
            if (regenTimer >= type.regenRateMs) {
                regenTimer = 0f
                if (unitCount < maxUnits) {
                    unitCount++
                }
            }
        }

        // Spawnovanie jednotiek pri útoku
        targetBase?.let { target ->
            if (sendingUnitsCount > 0) {
                spawnTimer += dtMs
                if (spawnTimer >= GameConfig.SPAWN_INTERVAL_MS) {
                    spawnTimer = 0f
                    if (unitCount > 0) {
                        unitCount--
                        sendingUnitsCount--
                        
                        // Uhlový rozptyl +- 30 stupňov
                        val toTarget = target.position.sub(position).normalize()
                        val angle = (Random.nextFloat() - 0.5f) * (Math.PI.toFloat() / 3f)
                        val cosA = cos(angle)
                        val sinA = sin(angle)
                        val spreadDir = Vector2(
                            toTarget.x * cosA - toTarget.y * sinA,
                            toTarget.x * sinA + toTarget.y * cosA
                        )
                        val spawnPos = position.add(spreadDir.mul(radius * 0.7f))
                        
                        val newUnit = Unit(
                            id = "u_${System.currentTimeMillis()}_${Random.nextInt(10000)}",
                            position = spawnPos,
                            ownerId = ownerId,
                            target = target,
                            sourceBase = this
                        )
                        onSpawnUnit(newUnit)
                    }
                    if (sendingUnitsCount <= 0 || unitCount <= 0) {
                        sendingUnitsCount = 0
                        targetBase = null
                    }
                }
            }
        }
    }

    fun sendUnitsTo(target: Base, ratio: Float = 1.0f) {
        if (unitCount <= 0) return
        val amount = (unitCount * ratio).toInt().coerceAtLeast(1)
        targetBase = target
        sendingUnitsCount += amount
        spawnTimer = GameConfig.SPAWN_INTERVAL_MS.toFloat() // Prvá jednotka vyrazí hneď
    }
}
```

### 10.3 `SteeringBehavior.kt`
```kotlin
package cdev.wargame.core

import cdev.wargame.entities.Base
import cdev.wargame.entities.Obstacle
import cdev.wargame.entities.Unit
import kotlin.math.max
import kotlin.math.min

object SteeringBehavior {
    fun calculateSteering(
        unit: Unit,
        allUnits: List<Unit>,
        bases: List<Base>,
        obstacles: List<Obstacle>,
        dtSeconds: Float
    ): Vector2 {
        val toTarget = unit.target.position.sub(unit.position)
        if (toTarget.magSq() == 0f) return Vector2(0f, 0f)
        val targetDir = toTarget.normalize()

        // 1. Separation
        var separation = Vector2(0f, 0f)
        var sepCount = 0
        for (other in allUnits) {
            if (other === unit || other.isDead || other.ownerId != unit.ownerId) continue
            val d = unit.position.distanceTo(other.position)
            if (d in 0.0001f..GameConfig.SEPARATION_RADIUS) {
                val diff = unit.position.sub(other.position).normalize()
                val factor = 1f - (d / GameConfig.SEPARATION_RADIUS)
                separation = separation.add(diff.mul(factor))
                sepCount++
            }
        }
        if (sepCount > 0) {
            separation = separation.div(sepCount.toFloat()).mul(GameConfig.SEPARATION_WEIGHT)
        }

        // 2. Odpudzovanie od cudzích základní s tangenciálnym obtekaním
        var baseRepel = Vector2(0f, 0f)
        for (base in bases) {
            if (base === unit.target) continue
            if (base === unit.sourceBase && !unit.hasLeftSourceBase) continue

            val toBase = base.position.sub(unit.position)
            val d = toBase.mag()
            val minDist = base.radius + unit.radius
            val avoidDist = minDist + GameConfig.BASE_AVOIDANCE_RADIUS

            if (d in 0.0001f until avoidDist) {
                val fromBase = unit.position.sub(base.position).normalize()
                val ratio = max(0f, (avoidDist - d) / (avoidDist - minDist))
                val intensity = ratio * ratio

                val radial = fromBase.mul(intensity * GameConfig.BASE_REPEL_WEIGHT)
                val cross = unit.velocity.x * toBase.y - unit.velocity.y * toBase.x
                val tangent = if (cross >= 0f) Vector2(-fromBase.y, fromBase.x) else Vector2(fromBase.y, -fromBase.x)
                val tangentForce = tangent.mul(intensity * GameConfig.BASE_TANGENT_WEIGHT)

                baseRepel = baseRepel.add(radial).add(tangentForce)
            }
        }

        // 3. Odpudzovanie od prekážok
        var obstacleRepel = Vector2(0f, 0f)
        for (obs in obstacles) {
            val toObs = obs.position.sub(unit.position)
            val d = toObs.mag()
            val minDist = obs.radius + unit.radius
            val avoidDist = minDist + GameConfig.OBSTACLE_AVOIDANCE_RADIUS

            if (d in 0.0001f until avoidDist) {
                val fromObs = unit.position.sub(obs.position).normalize()
                val ratio = max(0f, (avoidDist - d) / (avoidDist - minDist))
                val intensity = ratio * ratio

                val radial = fromObs.mul(intensity * GameConfig.OBSTACLE_REPEL_WEIGHT)
                val cross = unit.velocity.x * toObs.y - unit.velocity.y * toObs.x
                val tangent = if (cross >= 0f) Vector2(-fromObs.y, fromObs.x) else Vector2(fromObs.y, -fromObs.x)
                val tangentForce = tangent.mul(intensity * GameConfig.OBSTACLE_TANGENT_WEIGHT)

                obstacleRepel = obstacleRepel.add(radial).add(tangentForce)
            }
        }

        // Výsledný vektor riadenia
        var desiredDir = targetDir.mul(GameConfig.SEEK_WEIGHT)
            .add(separation)
            .add(baseRepel)
            .add(obstacleRepel)

        if (desiredDir.magSq() == 0f) desiredDir = targetDir

        val desiredVelocity = desiredDir.normalize().mul(unit.speed)
        val steer = desiredVelocity.sub(unit.velocity)
        return steer.mul(min(1f, dtSeconds * 16f))
    }
}
```

### 10.4 `GameSurfaceView.kt` (Vysokovýkonný herný cyklus)
```kotlin
package cdev.wargame.ui

import android.content.Context
import android.graphics.PixelFormat
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import cdev.wargame.core.GameEngine

class GameSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    @Volatile private var isRunning = false
    val engine = GameEngine()
    private val renderer = CanvasRenderer()

    init {
        holder.addCallback(this)
        holder.setFormat(PixelFormat.RGBA_8888)
        setOnTouchListener(GameInputHandler(engine, renderer))
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isRunning = true
        gameThread = Thread(this, "GameLoopThread").apply { start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        renderer.resize(width, height)
        engine.setupScreenBounds(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        gameThread?.join()
        gameThread = null
    }

    override fun run() {
        var lastTime = System.nanoTime()
        while (isRunning) {
            val now = System.nanoTime()
            val dtMs = (now - lastTime) / 1_000_000f
            lastTime = now

            if (!engine.isPaused) {
                engine.update(dtMs)
            }

            val canvas = holder.lockCanvas()
            if (canvas != null) {
                try {
                    renderer.render(canvas, engine.state)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
}
```

---

## 11. Zvukový subsystém (`SoundManager.kt`)

Pre Android Canvas verziu sa odporúča štandardný `SoundPool` so zvukovými efektmi v `res/raw/`:
- `snd_drag_select.ogg` (zvuk pri dotyku na vlastnú základňu)
- `snd_units_dispatched.ogg` (laserové svišťanie pri uvoľnení prsta)
- `snd_unit_clash.ogg` (drobná explózia pri vzájomnej zrážke jednotiek)
- `snd_base_reinforce.ogg` (príchod vlastnej jednotky domov)
- `snd_base_hit.ogg` (zásah štítu základne)
- `snd_base_captured.ogg` (triumfálne obsadenie základne)
- `snd_base_lost.ogg` (hlboký varovný tón pri strate základne)
- `snd_victory.ogg` a `snd_defeat.ogg`

---

## 12. Kontrolný zoznam pre overenie správnosti (QA Checklist)

1. [ ] **Škálovanie:** Pri akejkoľvek veľkosti displeja (telefón na výšku, tablet na šírku) je celé herné pole v strede a zachováva rovnaké proporcie.
2. [ ] **Touch Drag:** Dotknutie vlastnej základne vytvorí pulzujúci kruh; prejdenie ponad ďalšie vlastné základne ich pridá do výberu; pustenie nad nepriateľom/neutrálom vyšle vojská.
3. [ ] **Pohyb vojska:** Jednotky lietajú v kŕdli bez prekrývania, prirodzene obtekajú cudzie základne a skaly a nepresekávajú ich textúru.
4. [ ] **Regenerácia:** Neutrálne základne neprodukujú vojsko, vlastnené základne produkujú podľa svojho typu až do limitu.
5. [ ] **Výkon:** Plynulých 60 až 120 FPS aj pri 400+ jednotkách na obrazovke vďaka `SurfaceView` na dedikovanom vlákne.

---

## 13. Špecifikácia hlavnej obrazovky (Main Menu & Living Background)

Hlavná obrazovka (**Main Menu**) slúži ako centrálny rozbočovač hráča pred spustením misie. Vyniká prémiovým **Dark Cyber / Neo-Arcade** vizuálnym štýlom bez rozostrení (Zero Blur), s vysokým kontrastom a živým animovaným pozadím.

### 13.1 Vizuálna a priestorová štruktúra hlavnej obrazovky

Obrazovka je rozdelená na 3 vertikálne zóny:

```
+-------------------------------------------------------------+
| [TOP HUD]  🪙 2,450 | 💎 48          [⚙ Nastavenia]        | <- Fixná horná lišta (Safe Area padding)
+-------------------------------------------------------------+
|                                                             |
|   [● COMMANDER • LVL 12]                                    | <- Pulzujúci Rank Badge
|                                                             |
|           W A R G A M E . I O                               | <- Masívne logo s Cyan akcentom
|        REAL-TIME TACTICAL SWARM CONQUEST                    | <- Podtitul
|   ===============================================           | <- Gradientový svetelný oddeľovač
|                                                             |
|   +-----------------------------------------------------+   |
|   |                  PLAYER LEVEL                       |   |
|   |                    LVL 12                           |   | <- Karta hráča (Zlaté číslo, tieň)
|   |             12 SECTORS CONQUERED                    |   |
|   |   -----------------------------------------------   |   |
|   |   WIN RATE: 85.7%           STREAK: 4 WINS          |   |
|   +-----------------------------------------------------+   |
|                                                             |
|   +-----------------------------------------------------+   |
|   | LAST SECTOR 7      4,850 PTS   +150 🪙   +6 💎      |   | <- Súhrnná pilulka poslednej hry
|   +-----------------------------------------------------+   |
|                                                             |
|   +=====================================================+   |
|   |  ▶   L A U N C H   M I S S I O N                    |   | <- Hlavné tlačidlo (Breathing pulse)
|   +=====================================================+   |
|          DRAG FROM BASE TO ATTACK // SWARM CONQUEST         | <- Taktická inštrukcia
|                                                             |
| ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ |
| [LIVING BACKGROUND: Kozmický prach + Rotujúci taktický radar]| <- Beží v pozadí pod celým UI
+-------------------------------------------------------------+
| [BOTTOM TABS]   [MISSION (Active)]   [TECH]   [PROFILE]     | <- Fixná navigačná lišta
+-------------------------------------------------------------+
```

---

### 13.2 Detailný rozpad komponentov hlavnej obrazovky

#### 1. Horná lišta (Top HUD)
- **Umiestnenie:** Fixne navrchu, rešpektuje `safe-area-inset-top` (Notch padding minimálne $28 \cdot s$).
- **Výška:** $56 \cdot s + \text{safeTop}$.
- **Pozadie:** Vertikálny gradient zhora nadol (`rgba(0,0,0, 0.95)` $\rightarrow$ `transparent`).
- **Prvky:**
  - **Mena (Currency Pill):** Zaoblená pilulka (`border: 1px solid #222222`, podklad `#0A0A0A`).
    - Kredity: Ikona 🪙 + počet kreditov (napr. `2,450`), farba `#FFC629`.
    - Vertikálny oddeľovač: $1 \text{px}$ čiara `#333333`.
    - Diamanty: Ikona 💎 + počet drahokamov (napr. `48`), farba `#4DF1FF`.
  - **Tlačidlo nastavení (Settings Button):** Okrúhle tlačidlo s ozubeným kolesom (otvára modálne okno so zmenou jazyka EN/SK, zvukom SFX a resetom progresu).

#### 2. Karta loga a hodnosti (Header Section)
- **Rank Badge:**
  - Drobná pilulka s textom napr. `PLAYER LEVEL • LVL 12` (veľkosť písma $9 \cdot s$, písmo bold, farba `#4DF1FF`).
  - Vľavo svietiaca bodka ($4.5 \cdot s$), ktorá cyklicky pulzuje (`alpha: 0.6 -> 1.0`, `scale: 1.0 -> 1.2`).
- **Názov hry (Logo):**
  - Text `WARGAME` v snehovobielej farbe (`#FFFFFF`), veľkosť $36 \cdot s$, extra tučné písmo (`font-weight: 900`), letter-spacing $0.15\text{em}$.
  - Prípona `.IO` v žiarivej cyan farbe (`#00D2FF`) alebo zlatej (`#FFC629`).
- **Podtitul:** `REAL-TIME TACTICAL SWARM CONQUEST` (veľkosť $9 \cdot s$, farba `#888888`, tracking $0.25\text{em}$).
- **Oddeľovacia čiara (Separator):** Výška $1.5 \cdot s$, šírka $120 \cdot s$, horizontálny gradient (`transparent` $\rightarrow$ `#FFC629` $\rightarrow$ `transparent`).

#### 3. Štatistická karta hráča (Player Stat Card)
- **Rámček:** Obdĺžnik so zaoblenými rohmi ($10 \cdot s$), podklad `#121212`, tenký okraj `#222222`.
- **Obsah:**
  - Horný štítok: `PLAYER LEVEL` (veľkosť $10 \cdot s$, farba `#666666`, uppercase).
  - Hlavná hodnota: Veľké číslo úrovne (napr. `12`) v písme veľkosti $32 \cdot s$, zlatá farba `#FFC629` so svetelným tieňom `dropShadow(rgba(255, 198, 41, 0.35))`.
  - Podtitul: `12 SECTORS CONQUERED` (farba `#4DF1FF`, písmo bold $11 \cdot s$).
  - Spodný riadok: Rozdelený tenkou čiarou `#1E1E1E`:
    - Úspešnosť: `WIN RATE: 85.7%` (zelená `#4ADE80`).
    - Séria víťazstiev: `STREAK: 4 WINS` (zlatá `#FFC629`).

#### 4. Súhrnná pilulka sektora (Last Sector Pill)
- Tmavý horizontálny box (`#141414`), okraj `#222222`.
- Zobrazuje číslo naposledy hraného sektora a získané odmeny za posledný zápas (skóre, kredity, drahokamy).

#### 5. Hlavné tlačidlo "LAUNCH MISSION"
- **Rozmer:** Šírka 100% kontajnera (max $340 \cdot s$), výška $52 \cdot s$.
- **Vzhľad:** Žiarivý neónovo-zlatý podklad (`#FFC629`), text v sýto-čiernej farbe (`#0A0401`), ultra-tučné písmo $15 \cdot s$, tracking $0.12\text{em}$.
- **Animácia (Breathing Pulse):** Plynulý sínusový cyklus škálovania v trvaní 2.8 s:
  $$\text{scale} \in [1.0, 1.025], \quad \text{shadowBlur} \in [25 \cdot s, 45 \cdot s]$$
- **Inštrukcia pod tlačidlom:** Dvojriadkový subtílny text:
  `DRAG FROM BASE TO ATTACK` / `CONNECT MULTIPLE NODES TO SWARM` (veľkosť $9 \cdot s$, farba `#444444`).

#### 6. Spodná navigačná lišta (Bottom Tabs Bar)
- **Umiestnenie:** Fixne naspodku obrazovky, výška $84 \cdot s$, pozadie `#0B0B0B`, horný okraj `#222222`.
- **Taby:**
  1. **MISSION** (Ikona Radaru / Terča) – Spúšťa kampaň.
  2. **TECH** (Ikona Blesku ⚡) – Obrazovka vylepšení jednotiek (rýchlosť, regenerácia, obrana, kapacita).
  3. **ARMORY** (Ikona Štítu 🛡️) – Obrazovka skinov a profilu pilota.
- **Aktívny stav:** Vybraný tab je vizuálne vyvýšený, text svieti vo farbe `#FFC629` a má aktívny horný indikátor.

---

### 13.3 Živé taktické pozadie (`LivingBackground`)

Za celým rozhraním hlavného menu beží plynulá 2D grafická animácia pripravená pre natívny Canvas:

1. **Základná farba a gradient:**
   - Podklad: Hlboká čierna `#101010`.
   - Stredový radiálny gradient v dolnej tretine obrazovky:
     $$\text{Center} = (\text{width} / 2, \text{height} \cdot 0.65)$$
     $$\text{Radius} = \max(\text{width}, \text{height}) \cdot 0.7$$
     Farby: Zlatá žiara `rgba(255, 198, 41, 0.045)` v jadre $\rightarrow$ jemná cyan `rgba(77, 241, 255, 0.015)` $\rightarrow$ `#101010` na okrajoch.

2. **Kozmický kinetický prach (Floating Particles):**
   - 35 autonómnych svetelných častíc.
   - Pohyb: Pomalý stúpavý drift nahor ($v_y \in [-0.5, -0.15]$, $v_x \in [-0.12, 0.12]$).
   - Farby: 50% zlaté (`rgba(255, 198, 41, alpha)`), 30% cyan (`rgba(77, 241, 255, alpha)`), 20% biele.
   - Pri opustení horného okraja sa častica vráti na spodok s novou náhodnou X pozíciou.

3. **Rotujúci taktický vektorový radar (Cyber Radar Geometry):**
   - Rozmer: $380 \cdot s \times 380 \cdot s$, vycentrovaný horizontálne, spodkom zapustený pod okraj (`bottom: -15%`), nepriehľadnosť `opacity = 0.20`.
   - Rotácia: Konštantná pomalá uhlová rotácia (1 celá otáčka za 180 sekúnd).
   - Vektorové prvky:
     - Vonkajší prerušovaný kruh: Polomer $230 \text{px}$, farba `#FFC629`, dash `8, 12`.
     - Segmentovaný kruh: Polomer $190 \text{px}$, farba `#4DF1FF`, dash `40, 16, 20, 16`.
     - Šesťuholník radaru: 6 vrcholov s prerušovanou bielou čiarou `dash 6, 6`.
     - Prekrývajúce sa navigačné trojuholníky: Ružový `#F45588` a zelený `#4ADE80`.
     - Jadrové krúžky a zameriavací kríž (Crosshairs) pretínajúci stred.

---

### 13.4 Kotlin implementácia: `LivingBackgroundView.kt`

Túto triedu možno použiť ako pozadie hlavnej obrazovky v natívnom Androide:

```kotlin
package cdev.wargame.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class LivingBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Particle(
        var x: Float,
        var y: Float,
        val vx: Float,
        val vy: Float,
        val radius: Float,
        val color: Int
    )

    private val particles = mutableListOf<Particle>()
    private var radarAngle = 0f
    private var lastTime = System.nanoTime()

    private val bgPaint = Paint().apply { color = Color.parseColor("#101010") }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val radarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        particles.clear()
        val s = w / 350f
        repeat(35) {
            val hueType = Random.nextInt(3)
            val color = when (hueType) {
                0 -> Color.argb((Random.nextFloat() * 100 + 40).toInt(), 255, 198, 41) // Zlatá
                1 -> Color.argb((Random.nextFloat() * 100 + 40).toInt(), 77, 241, 255)  // Cyan
                else -> Color.argb((Random.nextFloat() * 80 + 30).toInt(), 255, 255, 255)
            }
            particles.add(
                Particle(
                    x = Random.nextFloat() * w,
                    y = Random.nextFloat() * h,
                    vx = (Random.nextFloat() - 0.5f) * 15f * s,
                    vy = (-Random.nextFloat() * 25f - 10f) * s,
                    radius = (Random.nextFloat() * 1.5f + 0.8f) * s,
                    color = color
                )
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.nanoTime()
        val dt = (now - lastTime) / 1_000_000_000f
        lastTime = now

        val w = width.toFloat()
        val h = height.toFloat()
        val s = w / 350f

        // 1. Tmavé pozadie
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // 2. Centrálny ambientný radiálny gradient
        val radialShader = RadialGradient(
            w / 2f, h * 0.65f, Math.max(w, h) * 0.7f,
            intArrayOf(
                Color.argb(12, 255, 198, 41),
                Color.argb(4, 77, 241, 255),
                Color.argb(240, 16, 16, 16)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        val gradPaint = Paint().apply { shader = radialShader }
        canvas.drawRect(0f, 0f, w, h, gradPaint)

        // 3. Kinetické častice
        for (p in particles) {
            p.x += p.vx * dt
            p.y += p.vy * dt

            if (p.y < -10f) {
                p.y = h + 10f
                p.x = Random.nextFloat() * w
            }
            if (p.x < -10f) p.x = w + 10f
            if (p.x > w + 10f) p.x = -10f

            particlePaint.color = p.color
            canvas.drawCircle(p.x, p.y, p.radius, particlePaint)
        }

        // 4. Rotujúci taktický radar
        radarAngle += (360f / 180f) * dt // 1 celá rotácia za 180 sekúnd
        canvas.save()
        val radarCenterX = w / 2f
        val radarCenterY = h * 0.85f // Umiestnený v dolnej časti
        val radarScale = (380f * s) / 500f

        canvas.translate(radarCenterX, radarCenterY)
        canvas.rotate(radarAngle)
        canvas.scale(radarScale, radarScale)

        // Prstence
        radarPaint.color = Color.argb(120, 255, 198, 41)
        radarPaint.strokeWidth = 2f
        radarPaint.pathEffect = DashPathEffect(floatArrayOf(8f, 12f), 0f)
        canvas.drawCircle(0f, 0f, 230f, radarPaint)

        radarPaint.color = Color.argb(100, 77, 241, 255)
        radarPaint.pathEffect = DashPathEffect(floatArrayOf(40f, 16f, 20f, 16f), 0f)
        canvas.drawCircle(0f, 0f, 190f, radarPaint)

        // Vnútorné jadro a kríž
        radarPaint.pathEffect = null
        radarPaint.color = Color.argb(80, 255, 255, 255)
        canvas.drawLine(-250f, 0f, 250f, 0f, radarPaint)
        canvas.drawLine(0f, -250f, 0f, 250f, radarPaint)

        canvas.restore()

        // Kontinuálne prekresľovanie pre 60 FPS animáciu pozadia
        postInvalidateOnAnimation()
    }
}
```

---

### 13.5 Odporúčaná architektúra celej obrazovky v natívnom Androide

V natívnom Androide existujú dve overené cesty pre hlavnú obrazovku:

1. **Hybridný prístup (Odporúčaný):**
   - V spodnej vrstve beží `LivingBackgroundView` (animované častice a rotujúci radar).
   - Nad ním je štandardný natívny layout (**Jetpack Compose** alebo **ConstraintLayout v XML**) so skutočnými komponentmi (`Button`, `TextView`, `CardView`).
   - *Výhoda:* 100% natívny prístup k systémovej prístupnosti (Accessibility), natívne animácie dotykov (Ripple effect), jednoduchá lokalizácia cez `res/values/strings.xml`.

2. **Čistý Canvas prístup (100% Canvas Game Screen):**
   - Celé menu (vrátane tlačidiel, kariet a textov) je vykresľované jedným `CanvasRenderer` cyklom, pričom dotyky sa spracovávajú cez matematické ohraničujúce obdĺžniky (`RectF.contains(x, y)`).
   - *Výhoda:* Nulová závislosť od Android UI frameworku, jednotné vykresľovanie identické s hernou obrazovkou.

