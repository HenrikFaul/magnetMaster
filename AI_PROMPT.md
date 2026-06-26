# Magnet Master — AI Fejlesztői Prompt

> **Tagline:** Pull it in. Slam it home. Feel the force.
> **Műfajok:** PHYSICS / ARCADE / SKILL / PUZZLE

Magnet Master egy side-view physics arcade, ahol ujj-mágnessel fémeket vonzol a goal-csúzdába. React + matter.js + custom magnet-force, Lovable Cloud daily leaderboard + asszinkron ghost-race, AI Gateway weekly procgen.

---

## 1. Vízió, célközönség, design

- **Vízió:** „A mágneses húzás-élmény a mobilon — szatisfaction in your fingertip."
- **Célközönség:** 12–40 év, férfi skew 58%, physics + arcade fans.
- **Top piacok:** USA, BR, IN, ID, RU, DE.
- **Design pillérek:** unique mechanic (no copycat), satisfying haptic, deep skill tree meta, leaderboard heat, boss spectacle.
- **Brand:** Steel Gray `#2B313A`, Electric Blue `#2F8DFF`, Safety Orange `#FF7A2A`, Bone `#F2ECE3`. Font: Fraunces + Inter Tight + JetBrains Mono.
- **KPI cél:** D1 ≥ 42%, D7 ≥ 20%, D30 ≥ 8%, ARPDAU ≥ 0.10, session 90s × 5/nap.

---

## 2. Core gameplay mechanika

### 2.1 Pálya

- Side-view 16:9 (világ-koord 1600×900).
- Statikus akadályok (lejtő, cső, fal), spawn-zónák fémhulladékkal.
- Goal-csúzda (chute) — ide kell becsúsztatni a fémet.

### 2.2 Mágnes

- Ujjpozíció = mágnes pozíció.
- Force `F = k · m · q / r²` (r-clamp 50 px alul, hogy ne legyen NaN).
- `k` skill-tree-vel skálázódik.
- Field-line render SVG-overlay (8 vonal kifelé sugározva).

### 2.3 Fémek

| Típus | Mass | KG-érték |
|-------|------|----------|
| Bolt | 1 | 0.05 |
| Coin | 0.5 | 0.10 |
| Gear | 4 | 0.40 |
| Plate | 8 | 1.00 |
| Wreckage | 20 | 2.50 |

### 2.4 Distractors

- Bomba: érintésre -1 HP (max 3 HP).
- Gumi: nem mágnesezhető, blokkol.
- Élesszélű: -0.5 HP.

### 2.5 Skill-tree (16 node)

| Ág | Node 1–4 |
|----|----------|
| Range | I, II, III, IV (mágnes-rádiusz +25% per) |
| Pull strength | I–IV (k +30% per) |
| Pulse | I–IV (3 sec-enként mega-pulse shockwave) |
| Polarity Flip | I–IV (tap-tap-pal taszít; cooldown rövidül) |

### 2.6 Boss-szintek

- 10-enként; multi-phase nagy roncs (autó, hajó, hajómotor).
- Phase 1: külső panelek lerippelése.
- Phase 2: belső reaktor magnetizálása.
- Phase 3: time-attack take-away.

### 2.7 Asszinkron PvP (Ghost Race)

- Heti random-mátch (skill-bracket).
- Saját + ellenfél „ghost" replay overlay.
- Win = +50 gem.

---

## 3. Frontend — React + matter.js

- **Stack:** React 19 + TS + Vite + matter.js + custom magnet-force plugin + Pixi/Canvas render + Howler + Zustand + framer-motion + TanStack Query.
- **Field-line render:** SVG overlay, 8 bezier-vonal a magnet-tól a 8 körirányba.
- **Haptics:** Capacitor Haptics light/medium/heavy minden „slam"-re.

```
src/games/magnet-master/app/
├── engine/
│   ├── PhysicsWorld.ts            # matter.js init
│   ├── MagnetForce.ts             # custom plugin
│   ├── ScoreCalc.ts
│   ├── SkillTree.ts
│   ├── ReplayRecorder.ts          # ghost race
│   └── Audio.ts
├── components/
│   ├── MagnetCursor.tsx
│   ├── FieldLines.tsx
│   ├── MetalSprite.tsx
│   ├── BombSprite.tsx
│   ├── ChuteSprite.tsx
│   ├── HUD.tsx
│   ├── SkillTreeScreen.tsx
│   └── LevelCompleteModal.tsx
├── screens/{Title,Gameplay,SkillTree,Daily,GhostRace,Shop,Settings}.tsx
├── data/{levels.json,skills.json,bosses.json}
├── store/gameStore.ts
└── lib/{ads,iap,analytics,i18n,haptics,share}.ts
```

- **Performance:** 60 FPS mid-Android @ 80 metal-body; low-Android 30 FPS @ 50.
- **Accessibility:** color-blind icons on bombs (X-shape), reduce-motion → field-line static.

---

## 4. Backend (Lovable Cloud)

### 4.1 Adattáblák

```sql
create table public.profiles ( /* standard */ );

create table public.user_progress (
  user_id uuid primary key references auth.users(id) on delete cascade,
  level_stars jsonb not null default '{}'::jsonb,
  skill_points int not null default 0,
  skill_levels jsonb not null default '{}'::jsonb,
  gems int not null default 0,
  coins int not null default 0,
  unlocked_skins text[] not null default array['default'],
  active_skin text not null default 'default',
  updated_at timestamptz not null default now()
);
grant select, insert, update on public.user_progress to authenticated;
grant all on public.user_progress to service_role;
alter table public.user_progress enable row level security;
create policy "self" on public.user_progress for all to authenticated
  using (auth.uid() = user_id) with check (auth.uid() = user_id);

create table public.daily_challenge (
  date date primary key,
  seed bigint not null,
  config jsonb not null,
  target_kg numeric not null
);
grant select on public.daily_challenge to authenticated, anon;
grant all on public.daily_challenge to service_role;

create table public.daily_leaderboard (
  id bigserial primary key,
  date date not null,
  user_id uuid not null,
  display_name text not null,
  kg_haul numeric not null,
  time_ms int not null,
  submitted_at timestamptz not null default now()
);
grant select, insert on public.daily_leaderboard to authenticated;
grant all on public.daily_leaderboard to service_role;
create index on public.daily_leaderboard (date, kg_haul desc, time_ms asc);

create table public.ghost_replays (
  id bigserial primary key,
  user_id uuid not null,
  level_id text not null,
  replay jsonb not null,           -- magnet positions @ 16ms
  kg_haul numeric not null,
  created_at timestamptz not null default now()
);
grant select, insert on public.ghost_replays to authenticated;
grant all on public.ghost_replays to service_role;
alter table public.ghost_replays enable row level security;
create policy "public read" on public.ghost_replays for select to authenticated using (true);
create policy "self insert" on public.ghost_replays for insert to authenticated with check (auth.uid() = user_id);

create table public.iap_receipts ( /* standard */ );
create table public.ad_events ( /* standard */ );
```

### 4.2 Edge Functions

- `POST /functions/v1/submit-daily` — replay-validate (re-simulate) + leaderboard írás.
- `GET  /functions/v1/daily-challenge`.
- `GET  /functions/v1/match-ghost` — heti opponent (skill-bracket alapján).
- `POST /functions/v1/grant-ad-reward`.
- `POST /functions/v1/validate-iap`.
- `POST /functions/v1/procgen-level` — AI Gateway weekly.

### 4.3 Procgen

```ts
const prompt = `Generate a Magnet Master level config.
Input: difficulty 1-100, environment 'industrial'|'junkyard'|'wreck'|'space'.
Output: {metal_spawns:[{type,x,y}], obstacles:[{kind,x,y,rot}],
target_kg, time_s, distractors:[{kind,x,y}]}`;
```

---

## 5. AI & intelligencia

### 5.1 Heti procgen

- +30 új szint AI Gateway. Server-side `kg_max` Monte-Carlo szim → balance check.

### 5.2 Skill-bracket matchmaking

- ELO-szerű 1200-base, ±100 toleranciával heti ghost-race párosítás.

### 5.3 DDA

- 3× fail → target_kg -10% (silent).
- Boss-2× fail → felajánl Pulse III próbát ingyen 1×.

### 5.4 Smart hint

- 5s tap-nélkül szint elején → finger-icon mutatja a legközelebbi metal-clustert.

### 5.5 Anti-cheat

- Replay = magnet-pos timeline.
- Edge re-simulate matter.js deterministically (fix dt 16ms, fix seed).
- Score mismatch > 3% → reject.

### 5.6 Recommendation

- 3× elköltött gem skill-pointra → ajánl „Engineer Bundle" IAP-t (50 gem + skin).

---

## 6. Monetizáció

### 6.1 Rewarded

| Key | Trigger | Reward |
|-----|---------|--------|
| `extra_time_15s` | gameplay | +15s |
| `2x_score` | level-end | dupla |
| `free_skill_point` | level-up | 1 SP |
| `emp_powerup` | boss | EMP egyszeri |
| `daily_wheel` | menu | gem / coin |

### 6.2 Interstitial

- 3 szintenként, capping 90s.

### 6.3 IAP

| Product | Tier | Ár | Tartalom |
|---------|------|----|---------|
| `gem.small` | cons | 0.99 | 100 gem |
| `gem.medium` | cons | 4.99 | 600 gem |
| `engineer.starter` | non-cons | 4.99 | 200 gem + skin + skill-reset |
| `skin.gold.magnet` | non-cons | 4.99 | gold skin |
| `skin.plasma` | non-cons | 2.99 | plasma skin |
| `noads` | non-cons | 2.99 | nincs interstitial |
| `battlepass.s1` | non-cons | 9.99/szezon | 30 step |
| `vip.subscription` | sub | 4.99/hó | no-ads + 1 daily skip + 50 gem/nap |

### 6.4 FTUE

| Step | Target |
|------|--------|
| app_open | 100% |
| first_pull | ≥ 98% |
| first_chute | ≥ 92% |
| first_skill_unlock | ≥ 70% |
| first_boss_win | ≥ 35% |
| first_iap | ≥ 2.8% |

### 6.5 LTV

`LTV(30) ≥ 1.0 USD T1`, `≥ 0.30 T3`.

---

## 7. ASO, lokalizáció, performance, launch

### 7.1 Kulcsszavak

| Piac | Primary | Secondary |
|------|---------|-----------|
| US | `magnet game`, `physics`, `arcade` | `pull`, `metal` |
| BR | `imã`, `física`, `arcade` | `puxar`, `metal` |
| IN | `magnet game`, `puzzle`, `physics` | `arcade` |
| DE | `magnet spiel`, `physik`, `arcade` | `ziehen` |

### 7.2 Store-listing

- Title: Magnet Master — Physics Arcade
- Subtitle: Pull, slam, master the force.
- Screenshots: title → gameplay field-lines → skill-tree → boss → leaderboard.
- Video: 30s satisfying-pull cuts + boss finale.

### 7.3 Lokalizáció: EN, PT-BR, ES, HI, ID, RU, DE, JA.

### 7.4 Viralitás

- Boss-finale share-clip auto-export.
- „I hauled 12.5 kg!" share-card.
- Ghost-race result share.

### 7.5 Push

- Daily challenge ends in 2h → local trigger.
- Ghost-race opponent submitted → instant.
- Streak about to break → local 20:30.

### 7.6 Soft launch

- BR, MX, PH, 3 hét, 200 USD/nap. Gate: D1 ≥ 38%, ARPDAU ≥ 0.06.

### 7.7 Global launch checklist

- Privacy, age 9+ (mild fantasy violence — bomba), GDPR, Sentry, asset-thinning, seasonal skin (Halloween magnetbat).

### 7.8 KPI

| Metrika | T1 | T3 |
|---------|----|----|
| D1 | 42% | 34% |
| D7 | 20% | 13% |
| D30 | 8% | 4% |
| ARPDAU | 0.10 | 0.03 |

### 7.9 Performance budget

- Android low: 280 MB, 30 FPS @ 50 body.
- Android mid: 360 MB, 60 FPS @ 80 body.
- iPhone 11+: 400 MB, 60 FPS @ 120 body.

### 7.10 QA

- Replay determinizmus 100% (fix-dt matter.js).
- Magnet-force NaN-safe (r-clamp).
- Ghost-race rendering 2 entity sync.

### 7.11 GDPR / COPPA: age-gate 13+; no UGC chat.

### 7.12 Post-launch roadmap

- +4h: Junkyard environment.
- +8h: Battle Pass S1.
- +12h: New skill: Pulse-EMP.
- +16h: Co-op tandem (asszinkron 2-magnet level).
- +24h: Tournament mode (real-time 1v1 best-of-3).

---

Sprintek:
1. matter.js + magnet-force prototype + 1 szint.
2. Field-lines render + chute + score.
3. 30 szint + skill-tree 16 node + cloud-save.
4. Boss-szintek + replay recorder.
5. Daily challenge + Edge anti-cheat + ghost-race.
6. IAP + rewarded + 6 skin + lokalizáció + Capacitor.

Siker: soft D1 ≥ 38%, ARPDAU ≥ 0.06, crash-free ≥ 99.5%, replay-determinizmus 100%.