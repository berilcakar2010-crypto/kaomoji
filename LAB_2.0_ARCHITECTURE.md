# Lab 2.0 — Architectural Assessment

Inspected: `app/` module, 6,661 LOC across 25 Kotlin files. Single JSON blob persistence
(`filesDir/state.json`), single-activity Compose UI, no DI, no local DB, no graph, no
curriculum contract — everything is an ad-hoc `data class` tree parsed by hand.

## 1. Classification of existing features

| Feature | Verdict | Why |
|---|---|---|
| Today's small task (`Mission.kt`) | REBUILD | Logic is sound (weighted heuristic over several signals) but it operates on a flat `Task` model keyed to curriculum JSON, not a knowledge graph. Keep the *heuristic-over-signals* idea, rewrite the inputs. |
| Curriculum (JSON-in-assets) | REPLACE WITH BETTER MODEL | Fixed schema (`phases→units→tasks`), no prerequisites, no relationships, no contexts. Becomes the **Curriculum Contract v1** import target. |
| Brain Inbox | KEEP (concept), REBUILD (storage) | Rapid capture without forced categorization is exactly right per spec §22. Move off the JSON blob into the new object store. |
| Projects | REBUILD | Current `ProjectDef` is static metadata + a `next` string. Needs research question, hypothesis, notes, experiments, outputs as real linked objects (§21). |
| Explanation/Feynman archive | KEEP (concept), REBUILD (links) | Recording-first UX is good. Needs to link to Concept/Context objects instead of a raw unit id string. |
| Mistake journal | KEEP (concept), EXPAND | Already soru/neden/doğru — add recurrence detection via the graph, link to Concept. |
| AI evaluation | KEEP (concept), REBUILD (depth) | Currently one prompt over a hand-built text summary. Needs structured input from the graph + capability-scoped AI calls. |
| Flashcards (SM-2) | KEEP (algorithm), REBUILD (links) | SM-2 math is correct and stays. Cards must reference Concept ids, not free text. |
| Stats (time-bucketed) | REBUILD | "Minutes studied" as primary metric contradicts §40. Keep the bucketing code, change what's measured. |
| Bridge graph | KEEP (concept) → becomes the real Knowledge Graph | Currently decorative (`Bridge` = name+desc+topic strings). This is the seed of §16 — promote it to first-class, queryable relationships. |
| Widget / notification | KEEP (concept), REBUILD (content) | Glance widget stays; what it shows changes (§43/§44). |
| D-pad focus (`DpadFocus.kt`) | KEEP as-is | Already a clean, reusable `Modifier`. No reason to touch it. |
| Foldable layout logic (`Root.kt`) | REBUILD | Was phone-width-break logic; tablet portrait-first needs different breakpoints (§8). |
| Theme (`Theme.kt`, Genç Araştırmacı palette) | KEEP palette tokens, REBUILD motion/elevation system | Colors/typography direction is right per §10; §11/§38 motion system doesn't exist yet. |
| `Store.kt` single JSON blob | REMOVE | Cannot support thousands of objects, graph queries, or migrations (§33/§36). |

## 2. New domain model (minimal, coherent — not the spec's exhaustive list taken literally)

One base shape, specialized by a `kind` discriminant, so relationships don't need N join tables:

```
KnowledgeObject
  id: String (UUID)
  kind: CONCEPT | QUESTION | SKILL | PRACTICE | DERIVATION | EXPLANATION | FLASHCARD |
        MISTAKE | RESOURCE | NOTE | IDEA | PROJECT | EXPERIMENT | EXAM | ASSIGNMENT |
        COURSE | CURRICULUM_UNIT | LEARNING_SESSION | OUTPUT
  title: String
  body: String?               // markdown; meaning depends on kind
  contextIds: Set<ContextId>  // School / AP / Olympiad / Research / Project ...
  createdAt, updatedAt: Instant
  schedule: Schedule?         // see below — optional, never required
  payload: JSON               // kind-specific structured fields (SM-2 state, mistake fields, etc.)

Relationship
  id, fromId, toId
  type: PREREQUISITE_OF | USED_IN | TESTS | CAUSED_BY | EXPLAINS | REINFORCES | ASSESSES | RELATED_TO
  note: String?

Context  (School, AP Physics, Olympiad, Research, Project X, ...)
  id, name, kind
```

This is the "smallest coherent model" the spec asks for in §15: everything the spec lists
(Concept, Question, Mistake, Project, Exam, …) is a `KnowledgeObject.kind`, not a separate
table — so one relationship engine (§16) serves all of them, and adding a new kind later
(handwritten note, citation, embedding — §50) never requires a schema migration, only a new
enum value and payload shape.

`Schedule` is deliberately a leaf, optional field, not the backbone (§2, §18):

```
Schedule
  suggestedDate: LocalDate?
  targetDate: LocalDate?
  deadline: LocalDate?
  examDate: LocalDate?
  priority: Int?
  estimatedEffortMin: Int?
  status: SUGGESTED | TARGET_SET | SCHEDULED | OVERDUE | COMPLETED | SKIPPED | PAUSED
```

Missing a `suggestedDate` never cascades into "damaged" state — there is no backlog table,
only a query ("objects whose targetDate < today and status is not COMPLETED/SKIPPED") that
the Advisor mode reads from and *offers* options on, per §18's example dialogue.

## 3. Persistence

Room (SQLite) replacing the JSON blob — justified against §33/§36/§53's "minimize
dependencies but don't sacrifice correctness": thousands of objects, graph joins, and FTS
search are not realistically done by hand over a JSON tree re-parsed on every read.

- `knowledge_objects`, `relationships`, `contexts` tables + Room's FTS4 virtual table over
  `title`/`body` for §30 global search.
- Room migrations from v1 onward — the JSON blob's one-time import becomes migration v0→v1.
- Attachments (audio, images) stay as files under the existing `FileVault` user-folder split
  (§34) — the DB stores paths/ids, never blobs.

## 4. AI provider abstraction (§6)

```
interface AIProvider {
  suspend fun complete(request: AIRequest): AIResult  // AIResult = Success | Failure | Offline
}
class GeminiProvider(apiKey: Provider<String?>) : AIProvider
class GroqProvider(apiKey: Provider<String?>) : AIProvider

class AICapabilityGate(private val provider: AIProvider?) {
  suspend fun writingAssist(...): AIResult   // wording/clarity/grammar only
  suspend fun proposePlan(...): AIResult     // Mode C only, never auto-applies
  suspend fun explainOrQuiz(...): AIResult
  suspend fun evaluateProgress(...): AIResult
  suspend fun organizeResearch(...): AIResult
}
```

`AiClient.kt`'s existing Groq/Gemini HTTP code is reusable almost as-is (KEEP) — the new work
is the `AICapabilityGate` layer so no call site can silently do more than its named capability
(§5), and a uniform `AIResult.Offline` so every screen has one code path for "no AI" (§7/§48)
instead of ad-hoc try/catch per screen as today.

**Correction from the original assessment**: `ApiKeyStore.kt` actually used plain
`SharedPreferences` (its own comment admitted this — "unencrypted, for simplicity"), not
`EncryptedSharedPreferences` as this document first claimed. That was a real gap against §35
("secure API-key storage"), fixed in Aşama 2: it now uses Android Keystore-backed
`EncryptedSharedPreferences`. Previously stored keys (if any) are orphaned by the prefs-file
rename and need re-entering once — a one-time, low-stakes cost for a single API key field.

## 5. Curriculum Contract v1 (§17/§52)

Versioned, external, importable JSON — the application never embeds subject content:

```
CurriculumPackage
  contractVersion: "1.0"
  domains: [{ id, name }]
  courses: [{ id, domainId, name, contexts: [SCHOOL|AP|OLYMPIAD|...] }]
  units: [{ id, courseId, title, objectives: [String], prerequisiteUnitIds: [String] }]
  concepts: [{ id, unitId, title, body }]
  questions: [{ id, conceptIds, prompt, kind: PREDICT|SOLVE|DERIVE|EXPLAIN, answer? }]
  assessments: [{ id, kind: EXAM|ASSIGNMENT, scopeConceptIds, suggestedDate? }]
  relationships: [{ fromId, toId, type }]
  resources: [{ id, title, url?, note? }]
```

Import = map each entry to a `KnowledgeObject` (+ `Relationship` rows) tagged with a
`sourcePackageId`, so a curriculum replace/re-import is "delete objects with this
sourcePackageId, re-insert" — never a hand-written migration. The *shape* here is this
project's contract to publish; the separate curriculum-generation project targets it.

## 6. Offline/online boundary (§7)

Everything above (Room, relationships, scheduling, stats, search, flashcards, mistakes,
notes, projects, recordings) is pure local I/O — zero network calls. The only network-capable
calls are the four `AICapabilityGate` methods, each going through `AIProvider`, each returning
`AIResult.Offline` immediately (no silent queueing — §35) when no provider is configured or
the request fails. No screen blocks on AI; AI panels render as an optional, collapsible
section that shows "Gemini şu an ulaşılamıyor — yerel çalışma alanın kullanılabilir." (§48)
instead of a spinner-forever or a crash.

## 7. Portrait-tablet UI structure (§8/§31/§32)

Single-activity Compose, but the current phone-width `Root.kt` bottom-bar/rail breakpoint
logic is replaced with three tiers tuned to tablet portrait as the primary target:

- **Compact** (foldable closed, <380dp): today's single-line focus, same spirit as current.
- **Tablet portrait (primary, 600–900dp width)**: persistent left rail (icons+labels, not a
  bottom bar) + content column sized for one-handed-with-stylus notebook use; detail views
  open as a sheet that slides up from the bottom rather than a full navigation push, so
  context (what you came from) stays visible per §38.
  Yes, this means any decision between a bottom bar and a side rail defaults to the rail even
  in portrait — a notebook's spine is on the side, not the bottom.
- **Tablet landscape / unfolded (>900dp)**: rail + content + a persistent secondary panel
  (today's focus / graph neighbors / AI panel), matching the existing foldable-open behavior.

Navigation areas (§31, derived from the domain model, not copied verbatim from the spec):
**Lab** (home/command surface), **Learn** (active learning session engine, §12–14),
**Knowledge** (graph + search + all object kinds), **Projects**, **Academics** (exams,
assignments, contexts), **Archive** (recordings, mistakes, flashcards review, stats).

## 8. What this assessment deliberately does NOT settle yet

Given the scope, two things need your call before I start writing code (see question below):
delivery shape (in-place vs. parallel) and phasing. Everything else above I'm treating as
decided and will build against.

## 9. Status after Aşama 1–7 (final pass for this round)

Built, tested, CI-green on every phase, additive (the old `app/` screens never stopped
compiling or working at any point):

- **Aşama 1** — domain model (`KnowledgeObjectEntity`/`RelationshipEntity`/`ContextEntity`),
  Room, Curriculum Contract v1 + legacy-JSON adapter.
- **Aşama 2** — `AIProvider`/`AICapabilityGate` (capability-scoped, offline-safe), real fix to
  `ApiKeyStore` (plaintext → `EncryptedSharedPreferences`).
- **Aşama 3** — first real screen (Lab 2.0 home: quick capture, upcoming/overdue-as-option,
  recent objects), backed by the real repository, real empty states.
- **Aşama 4** — question-first learning engine (§12–14): discipline-adaptive stage sequencing,
  attempt text preserved across every transition.
- **Aşama 5** — portrait-tablet nav shell (COMPACT/TABLET_PORTRAIT/TABLET_LANDSCAPE), landscape
  secondary panel so opening a session/graph never hides where you came from.
- **Aşama 6** — the user's real 123-object curriculum package imported end to end (hierarchy,
  typed prerequisite edges, cross-discipline connections, context mappings), nothing silently
  dropped (full raw object kept in `payload`).
- **Aşama 7** — the knowledge graph's first real UI: a navigable prerequisites/enables/related
  screen, not a decorative diagram.
- **Aşama 8** — `AICapabilityGate` (built in Aşama 2, unused until now) wired into
  `ConceptGraphScreen` ("explain this concept") and a new `LabEvaluationScreen` (real counts
  first, AI commentary as a separate, clearly-labeled block). `LabRepository.search()` (built
  in Aşama 1, unused until now) got its first screen, `LabSearchScreen`.
- **Aşama 9** — mistake journal and flashcards (SM-2) rebuilt on the new model.
  `MistakePayload`/`FlashcardPayload` existed since Aşama 1 with no caller; now
  `MistakeJournalScreen` (problem/attempt/what-went-wrong/why/correct-reasoning/category,
  plus a non-judgmental recurring-category note) and `FlashcardReviewScreen` (a real SM-2
  engine ported to the new payload shape, not reusing the old `data/SM2.kt` which the old
  screens still depend on) both work end to end.
- **Aşama 10** — data ownership (§34/§41): `LabDataScreen` exports every object/relationship/
  context the user has produced in Lab 2.0 to one plain JSON file via SAF
  (`CreateDocument`/`OpenDocument` — no new permission, no server), and restores from one.
- **Aşama 11** — `ProjectsScreen` (§21: research question fixed, notes/next-action evolve,
  "SIRADAKİ EYLEM" is the headline, not a task list) and `ExamsScreen` (§20: scope + prep
  status, date lives in the same `Schedule` the home screen's "upcoming" query already reads
  — creating an exam here makes it show up there with zero extra wiring). README.md now
  points to Lab 2.0 and this ledger; before this it was undiscoverable outside the app itself.
- **Aşama 12** — the last two unused `AICapabilityGate` methods got callers: `improveWriting`
  in `ProjectsScreen` (notes field — result shown separately, the user taps "Bu metni kullan"
  to adopt it, it never overwrites silently, per §5) and `proposeStudyPlan` in
  `LabEvaluationScreen` ("7 Günlük Plan Öner" — a suggestion only, nothing in this call path
  can write to a Schedule). All five `AICapabilityGate` methods now have a real caller.
- **Aşama 13** — Lab 2.0's own home-screen widget (`Lab2Widget`/`Lab2WidgetReceiver`, new
  `lab2_widget_info.xml`): due flashcard count + nearest upcoming exam/assignment. The old
  `MissionWidget` (bound to `Store.kt`) was not touched or reused — this is a parallel widget,
  same non-destructive pattern as everything else in Lab 2.0.
- **Aşama 14** — old data migration (§34): a new `LegacyDataMigrator` object (pure mapping
  functions, no I/O) converts every `Store.kt` collection — mistakes, flashcards, recordings
  (anlatım/transcript+analysis), Brain Inbox notes, practice-problem logs, weekly reviews,
  project states, assessment states — into the new knowledge graph, namespaced with a
  `legacy-*` id prefix and `sourcePackageId = "legacy-migration"` so re-running it is safe
  (REPLACE, not duplicate). `LabRepository.migrateLegacyData(context)` instantiates a
  read-only `Store(context)`, does the mapping, and writes through the existing
  `dao.importPackage(...)` transaction. `LabDataScreen` got a new "Eski Verimi Kopyala"
  button with an explanatory note and a per-category count summary. `Store.kt` itself was
  never written to, modified, or put at risk — this is a one-way read, exactly like every
  other non-destructive step in this project. 8 unit tests cover the mapping logic
  (`LegacyDataMigratorTest`).
- **Aşama 15 — full cutover: Lab is now the whole app.** The user explicitly asked to stop
  running two systems side by side and make the knowledge-graph app ("Lab") the only app,
  with its own identity. This phase is the one genuinely destructive step in the whole
  project, done deliberately and only on explicit instruction:
  - **Deleted entirely** (old curriculum-bound UI, now unreachable from anywhere): `ui/Root.kt`,
    `ui/GardenScreen.kt`, `ui/OtherScreens.kt` (Inbox/Projects/Mistakes/Assessments/Review/
    Storage/StudyBag/Resources/ExplainIt), `ui/AudioScreens.kt`, `ui/CurriculumScreen.kt`,
    `ui/CurriculumEditScreen.kt`, `ui/CurriculumGenScreen.kt`, `ui/EvaluationScreen.kt`,
    `ui/FlashcardsScreen.kt`, `ui/BridgeGraphScreen.kt`, `ui/StatsScreen.kt`,
    `audio/Audio.kt` (Recorder/Player), `storage/FileVault.kt`, `storage/DocumentTextExtractor.kt`,
    `widget/MissionWidget.kt`, `widget/MissionNotifier.kt`, `data/Mission.kt`,
    `ai/AiClient.kt` (superseded by `ai/engine/AIProvider`).
    Dead decorative helpers only those screens used (`TaskRow`, `Checkbox`, `Sheet`, `Chip`,
    `Sticker`, `Card`, `subjectColor`, `BodySoft`, `gingham`, `dashed`) were removed from
    `Widgets.kt`/`Theme.kt` too. `RECORD_AUDIO` and `POST_NOTIFICATIONS` permissions, the
    `FileProvider` manifest entry, and the unused `pdfbox-android`/`documentfile` dependencies
    were removed since nothing left uses them.
  - **Deliberately kept, unmodified**: `data/Store.kt`, `data/Models.kt`, `data/CurriculumLoader.kt`,
    `data/CurriculumSerializer.kt` — not because they're still reachable from any screen (they
    aren't), but because `LegacyDataMigrator` (Aşama 14) needs a working `Store(context)` to
    read old on-device data from. This is the one remaining purpose of the entire old data
    layer: a one-time, read-only migration source for people upgrading from the pre-cutover
    app. `data/SM2.kt` (old, millisecond-based) also had to be kept, for a sharper reason than
    the others: a first attempt at this cutover deleted it, assuming (wrongly) that only the
    deleted `FlashcardsScreen` called it — `Store.kt`'s own `reviewFlashcard()` calls
    `SM2.hesapla(...)` directly, unqualified, in the same package, which a qualifier-based
    grep (`data.SM2`) missed. CI caught the resulting `Unresolved reference 'SM2'` immediately;
    the file was restored from git history rather than touched. `ai/AiProvider.kt` (enum),
    `ai/GeminiClient.kt`, `ai/GroqClient.kt`, `ai/ApiKeyStore.kt`, `ai/CurriculumPrompt.kt` were
    kept because `ai/engine/*` (the new, capability-gated AI layer) wraps them directly — these
    were never old-UI-only code.
  - **`MainActivity`** rewritten from scratch: no more `Store`/`Recorder`/`Player`/permission
    dance — it does nothing but `setContent { LabTheme { Lab2Root() } }`. `Lab2Root()` lost its
    `onExit` parameter (there is no other app to exit to); `Lab2HomeScreen` lost its "Geri"
    button and its "(Beta)"/"ayrı giriş noktası" framing — it's described as what it now is,
    the app's home screen, not a parallel experiment.
  - **App identity**: `app_name` → **"Lab"**; `applicationId` → **`com.beril.lab`** (was
    `com.beril.kaomoji` — this makes it, from Android's and the Play Store's point of view, a
    different app; an existing `com.beril.kaomoji` install cannot be updated in place by this
    APK, it installs side by side). The Kotlin package namespace (`com.beril.kaomoji`) was
    **not** renamed — renaming every file's package for a cosmetic-only change across ~50
    files was assessed as pure risk with no functional benefit, so `namespace` and
    `applicationId` now intentionally differ (a normal, fully-supported Android/AGP pattern).
    `Theme.Kaomoji` → `Theme.Lab`, `KaomojiTheme` → `LabTheme`. The `Lab2*` prefix on internal
    classes (`Lab2Root`, `Lab2Widget`, `Lab2Breakpoint`, package `ui.lab2`) was **not** renamed
    to drop the "2" — purely cosmetic internal naming, same reasoning.
  - The old widget (`MissionWidget`) is gone; `Lab2Widget` is now the app's only widget, and
    absorbed `MissionWidget`'s `OpenAppAction` (the only piece of it that had a second caller).
  - `README.md` rewritten to describe Lab as it actually is today, not the old curriculum app.
    `GUNCELLEME_NOTLARI.md` and `UYGULAMA_TANITIMI.md` (both purely about the deleted system)
    were deleted rather than left stale.
- **Aşama 16** — curriculum auto-import + lock-screen notification (§44), in response to the
  user's "attığım müfredatı entegre et" (integrate the curriculum I sent).
  - `LabRepository.ensureDefaultCurriculumImported(context)`: if the personal curriculum
    package (`lab2-personal-curriculum`, 123 objects, real prerequisite graph, cross-discipline
    connections, context mappings) hasn't been imported yet, imports it — called once from
    `Lab2HomeScreen`'s existing `LaunchedEffect(Unit)`. Before this, the only on-ramp was a
    button on the home screen the person had to find and tap; now Lab's own content is simply
    there on first open, matching what "Lab is the whole app" (Aşama 15) actually implies. The
    manual button stays, relabeled "Müfredatı Yenile" — useful if a future app update ships a
    revised `lab2_curriculum.json` and the on-device copy needs to be refreshed (the import is
    idempotent either way — REPLACE by `sourcePackageId`, never duplicates).
  - **Lock-screen notification** (closes the §44 gap listed below as of Aşama 15): a new
    `LabNotifier` object (same non-destructive, additive pattern as everything else here)
    shows a persistent, PUBLIC-visibility notification with the same content `Lab2Widget`
    already computes — nearest overdue/upcoming target, else due-flashcard count — so no new
    query was written, just a second consumer of `pastTargetDate`/`upcoming`/`dueFlashcards`.
    Refreshed once in `MainActivity.onCreate` and again whenever `Lab2HomeScreen` bumps its own
    `refreshTick` (capture, concept creation, curriculum import, etc. already trigger that).
    `POST_NOTIFICATIONS` permission and the `ic_notification` drawable (both removed in
    Aşama 15 along with the rest of the old `MissionNotifier`) came back — this is genuinely
    new functionality built on the new model, not a port of old code; the old
    `widget/MissionNotifier.kt` file itself stays deleted.

### Honestly still NOT built (not a short list — said plainly, not glossed over)

- **Old data migration is partial, not full parity**: `Store.kt`'s fine-grained
  curriculum-progress fields — `done`/`dailyLogs`/`problems`-level per-task completion stats
  tied to the OLD curriculum's own task ids — are deliberately NOT migrated (Aşama 14). Those
  old task ids don't map to anything in the new knowledge graph, and a fake mapping would be
  worse than an honest gap. What IS migrated: mistakes, flashcards, recordings, Brain Inbox
  notes, practice logs, weekly reviews, project/assessment state. This is a one-time copy a
  person triggers once from "Verim", not a live sync — there is no second system to sync with
  anymore after Aşama 15.
- **Motion/micro-interactions** (§11/§38) — screens render instantly with no transition design;
  the spec's "small, satisfying animations" are not implemented.
- **A dedicated top-level nav area per §31** (Learn/Knowledge/Projects/Academics/Archive as
  separate rail destinations) — Lab is one screen with many sub-screens reachable from it,
  not six permanent areas. This is now a single-system gap, not a parallel-system one: there
  is exactly one app, and this is about its internal nav shape, not about a second app to
  reach it through (Aşama 15 removed that door entirely — Lab is the whole app now).
- **Internal `Lab2*` naming** (`Lab2Root`, `Lab2Widget`, `Lab2Breakpoint`, package `ui.lab2`)
  still carries the "2" from when this coexisted with an "old Lab" — purely cosmetic, listed
  here for honesty rather than silently left unmentioned.

None of this is secretly done — it's the honest remainder of a 55-section spec against sixteen
phases in one session. What exists is real (compiles, is tested, is CI-verified, is not a
mockup) for the slice it covers; the slice is a meaningful fraction, not the full vision, and
claiming otherwise would be dishonest.
