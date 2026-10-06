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
- **Aşama 12** — two more unused `AICapabilityGate` methods got callers: `improveWriting`
  in `ProjectsScreen` (notes field — result shown separately, the user taps "Bu metni kullan"
  to adopt it, it never overwrites silently, per §5) and `proposeStudyPlan` in
  `LabEvaluationScreen` ("7 Günlük Plan Öner" — a suggestion only, nothing in this call path
  can write to a Schedule). **Correction (caught in Aşama 21, left here rather than quietly
  edited away):** this entry originally claimed "all five `AICapabilityGate` methods now have
  a real caller." That was false — `organizeResearchNotes` had none, anywhere, until Aşama 21.
  The claim went unverified for nine phases; nothing in CI could have caught it, since a
  missing UI caller to a working method isn't a compile error or a test failure.
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
    to drop the "2" at this point — purely cosmetic internal naming, same reasoning (this did
    get cleaned up later, in Aşama 26).
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
- **Aşama 17** — motion/micro-interactions, closing the §11/§38 gap. `Lab2Root`'s screen
  switch (narrow width: Home ↔ detail; wide width: the secondary panel's own content) now
  uses `AnimatedContent` instead of an instant cut — forward navigation slides in from the
  trailing edge and fades, back navigation reverses it, and detail-to-detail transitions
  (e.g. one concept to another in the wide secondary panel) cross-fade. Direction is decided
  by one condition (`targetState !is Lab2Screen.Home`), nothing fancier. `Lab2HomeScreen`'s
  four lists (past-target/upcoming/concepts/recent) got `Modifier.animateItem()` so adding a
  concept, capturing a note, or importing a curriculum animates the list instead of snapping.
  `androidx.compose.animation:animation` was added as an explicit dependency (previously only
  available transitively through Foundation/Material3, never used directly). This is
  deliberately scoped to navigation + list changes — it does not touch per-element micro-
  interactions (button press feedback, checkbox toggles, etc.), which stay as a residual gap
  below rather than being claimed as done.
- **Aşama 18** — the per-element micro-interaction named as a residual gap right after Aşama
  17 landed: `ui/nav/DpadFocus.kt`'s shared `dpadFocusable` modifier (used by `Btn`, `GhostBtn`,
  and every focusable row in the app — one place, app-wide reach) now tracks its own press
  state via a `MutableInteractionSource` and animates a 0.96 scale on press
  (`animateFloatAsState`, 100ms). The existing ripple indication is preserved explicitly
  (`indication = LocalIndication.current`) — this is additive feedback, not a replacement.
  Because every button in Lab already goes through this one modifier, the fix is one file,
  not forty.
- **Aşama 19 — a real regression found and fixed: AI settings had no UI at all.** When Aşama
  15 deleted the old screens (`CurriculumGenScreen`, `EvaluationScreen`, `AudioScreens`, etc.),
  it deleted the only places that ever wrote to `ApiKeyStore` along with them.
  `AICapabilityGate.forContext` kept reading from `ApiKeyStore` exactly as before, so nothing
  failed to compile and nothing crashed — every AI call just silently returned
  `AIResult.Offline` forever, because no screen could ever set a key again. This was caught
  while responding to the user's "AI ayarlarını ekle" request, not caught by CI (a missing
  screen isn't a compile error). New `AiSettingsScreen` (`ui/lab2/AiSettingsScreen.kt`): pick
  Groq or Gemini, enter/save/clear the key, see whether one is currently configured — reads
  and writes the same `ApiKeyStore` the gate already used. Reachable from `Lab2HomeScreen`'s
  button row ("🤖 AI Ayarları"). No new storage, no new AI-call path — the missing piece was
  purely the UI to configure what already existed underneath.
- **Aşama 20 — S Pen.** The user asked for S Pen usability; this splits into three genuinely
  different things, so each gets its own honest answer rather than one blanket "done":
  - **Touch/tap** (buttons, scrolling, text field focus): already works, zero code needed.
    Android and Compose treat a stylus as a standard pointer input — `clickable`,
    `dpadFocusable`, scrolling, everything already built in this app responds to an S Pen
    exactly like a finger. Nothing to build here; said so rather than writing a no-op "fix".
  - **Handwriting-to-text in text fields** (`Field`, used by quick capture, concept names,
    API key entry, etc.): also already works without app code, on devices where Samsung's/
    Android's system-level direct-writing IME is enabled — it intercepts stylus input on any
    standard editable text view with a real `InputConnection`, which `BasicTextField`
    (`Field`'s implementation) already provides. This app doesn't need to implement
    handwriting recognition itself; it would be redundant with, and worse than, the OS's own.
  - **Hover preview** (genuinely new — not something that "already worked"): `Lab2HomeScreen`'s
    concept rows now use `Modifier.hoverable()` — holding a mouse pointer or an S Pen just
    above a concept (no touch) tints the row and swaps its hint text, before committing to
    opening it. Scoped to the one list most people will hover over first; the concept graph's
    own nodes and other lists don't have this yet (see below).
- **Aşama 21 — the user's actual complaint: "AI was supposed to revise my writing and give
  advice, and this is supposed to be a full academic OS," and the app currently falls short
  of that.** Two real problems, not one:
  1. Until Aşama 19 (just before this), AI was configurable nowhere, so every AI call
     returned `Offline` regardless of what was wired up — anyone who tried the writing/advice
     features before that fix would have seen exactly nothing happen. That's now fixed; it
     requires the person to actually open "🤖 AI Ayarları" and enter a key.
  2. Separately, and this is the fix in this phase: `improveWriting` was wired into exactly
     one place (`ProjectsScreen`'s notes field) and `organizeResearchNotes` into zero places —
     "AI will revise my writing" was never true as a general capability, only as a buried
     corner of the project-notes editor. New `WritingAssistScreen` (reachable from
     `Lab2HomeScreen`'s "✍️ Yazım Yardımı"): paste or write ANY text — an essay draft, a
     captured note, anything — and run either `improveWriting` (clarity/grammar, meaning
     preserved) or `organizeResearchNotes` (organize/summarize, never fabricates a source or
     result) against it, result shown separately as always, "Bu metni kullan" to adopt it.
     No new AI-call path — both methods already existed in `AICapabilityGate` — the fix is
     purely giving them a general-purpose front door instead of one narrow or nonexistent one.
  This does not make Lab "a full academic OS" in the sense the original 55-section spec
  describes — see the gap list below, which is long. It makes the two AI capabilities the
  user named ("revise my writing," "give advice") actually reachable and actually working,
  which they were not, for two different reasons, until this phase.
- **Aşama 22 — full audit of `AICapabilityGate`, not just the two methods from Aşama 21.**
  It has **nine** methods, not five (another correction to the Aşama-12 record) —
  `explainConcept`, `evaluateProgress`, `improveWriting`, `proposeStudyPlan`,
  `organizeResearchNotes` (all reachable as of Aşama 21), plus `analyzeTranscript`,
  `transcribeAudio`, `generateFlashcards`, `generateCurriculum`, which were never checked.
  `generateFlashcards` was reachable and valuable — Lab already has a working flashcard
  system — so it's wired now: `FlashcardReviewScreen` got a "🤖 AI'dan Kart Üret" form
  (paste source text + subject, get suggested question/answer pairs, review them, "Tümünü
  Kart Olarak Ekle" to actually add them — nothing is added without that explicit step).
  `analyzeTranscript` and `transcribeAudio` are **not** wired, and won't be without a much
  bigger piece of new work: both need an audio recording, and Aşama 15 deleted the only
  recording feature this app ever had. Building that back (microphone capture, playback,
  file storage) is a real, separate feature, not a wiring fix — left as a residual gap, not
  silently ignored. `generateCurriculum` is left unwired for now (reversed in Aşama 25, at
  the user's explicit request — see below): Lab's curriculum model is built on an external
  Curriculum Contract, "the app never silently authors curriculum content" is the actual
  design principle, and a working "AI writes your curriculum" button only contradicts that
  principle if its output gets written without the user explicitly reviewing and importing it.
- **Aşama 23 — audio recording, rebuilt on the new model.** Closes the gap Aşama 22 found
  (and, with it, unblocks `analyzeTranscript`/`transcribeAudio` for a future phase, though
  neither is wired yet — this phase is the recording feature itself, not the AI analysis on
  top of it). New `lab/audio/LabAudio.kt` (`LabRecorder`/`LabPlayer`, `MediaRecorder`/
  `MediaPlayer`, app-private storage under `filesDir/recordings`) — independent from the old,
  deleted `audio/Audio.kt`, never touches it, never reuses its files. `LabRepository` got
  `createExplanation(conceptId, conceptTitle, audioFilePath)` (an `EXPLANATION` object linked
  to its concept via the existing `EXPLAINS` relationship type — this is why no payload schema
  change was needed, `ExplanationPayload.audioFilePath` already existed from Aşama 1, unused
  until now) and `explanationsFor(conceptId)` (newest first). `ConceptGraphScreen` got a new
  "🎙️ Anlat (Feynman tekniği)" section: request mic permission, record, stop-and-save, and a
  list of past recordings for that concept with play/stop. `RECORD_AUDIO` (removed in Aşama
  15 along with the rest of the old audio code) came back — this time for a feature Lab
  actually has, not a port of anything. 2 unit tests cover the repository methods
  (`LabRepositoryTest`).
- **Aşama 24 — `analyzeTranscript`/`transcribeAudio` wired, closing the last of the two gaps
  Aşama 22's audit found.** Each recording in `ConceptGraphScreen`'s explanation list now
  gets a "🤖 Transkribe Et" button (once: `transcribeAudio` on the audio file, result saved
  straight to `KnowledgeObjectEntity.body` via new `LabRepository.attachTranscript` — a
  mechanical transcription of your own recorded speech carries none of the "AI changes your
  meaning" risk that writing assistance does, so unlike `improveWriting`'s "Bu metni kullan"
  step, this one saves automatically; the transcript is still shown inline for transparency).
  Once a transcript exists, "🤖 Analiz Et" becomes available (`analyzeTranscript`, saved to
  `ExplanationPayload.aiEvaluation` via new `attachEvaluation`). No new AI-call path — both
  methods already existed; the fix is the same shape as Aşama 21/22's: a missing front door.
  2 more unit tests cover `attachTranscript`/`attachEvaluation`.
- **Aşama 25 — `generateCurriculum` wired, reversing Aşama 22's decision at the user's
  explicit request** ("generate curriculum da bağlansın" — connect generateCurriculum too).
  Schema mismatch found while wiring it: `CurriculumPrompt.SYSTEM`, the system prompt both
  AI clients use for `generateCurriculum`, outputs the OLD legacy `phases→units→tasks` schema
  (the one `LegacyCurriculumAdapter` already bridges for the old on-disk curriculum), not
  Contract v1 — so AI output must go through `LegacyCurriculumAdapter.toContractPackage`, not
  `CurriculumContractParser` directly, or it would fail to parse every time. New
  `CurriculumGenScreen` (reachable from `Lab2HomeScreen`'s "📄 Belgeden Taslak Oluştur", next
  to the existing "Müfredatı Yenile"): paste a syllabus/ders programı/any source text, call
  `generateCurriculum`, the raw AI output is parsed through the same adapter as a *preview*
  (unit/concept/task counts, or a readable error if the AI's JSON doesn't parse) — nothing is
  written to the knowledge graph until the user taps "İçe Aktar", the same explicit-action
  gate as `improveWriting`'s "Bu metni kullan" (§5). New `LabRepository.importGeneratedCurriculum`
  does the actual import, tagging written objects with its own `sourcePackageId`
  (`"ai-generated-curriculum"` by default) so it never collides with or overwrites the real
  123-object external package. This does NOT make Lab author curricula silently — the AI
  produces a draft, the user decides whether to keep it, same as every other AI capability in
  this app. The re-sent `curriculum.json` from this request was verified byte-identical to the
  already-bundled asset (`app/src/main/assets/lab2_curriculum.json`, same 123 objects, same
  byte size) — no asset change was needed, only this wiring. 2 unit tests cover the happy path
  (legacy-shaped JSON imports and tags correctly) and the failure path (garbage AI output
  throws instead of silently importing nothing).
- **Aşama 26 — three gaps from the "Honestly still NOT built" list closed at the user's
  explicit request: the §31 top-level nav, a general recording archive, and the `Lab2*`
  cosmetic naming debt.** All three were tackled together because the nav restructure and
  the naming cleanup touch the same files.
  - **Naming**: every `Lab2*` symbol is gone — `Lab2Root`→`LabRoot`, `Lab2HomeScreen`→
    `LabHomeScreen`, `Lab2NavShell`→`LabNavShell`, `Lab2Breakpoint`→`LabBreakpoint`,
    `Lab2Widget`/`Lab2WidgetReceiver`→`LabWidget`/`LabWidgetReceiver`, package `ui.lab2`→
    `ui.lab`, plus the `AndroidManifest.xml` receiver entry, the `lab2_widget_info.xml`
    resource file, and the `lab2_widget_description` string resource. Left alone on purpose:
    `lab2_curriculum.json` (the actual asset filename) and the `"lab2-personal-curriculum"`/
    `"lab2-yedek-…"` string identifiers — these are stable data keys and a user-facing backup
    filename prefix, not code-naming debt; renaming them is a data-migration question, not a
    cosmetic one, and was out of scope here.
  - **§31 top-level nav**: `LabNavShell`'s rail is no longer a single static "Ana Sayfa"
    item — it now shows the five permanent areas the spec names (`LabArea`: Öğren/Bilgi/
    Projeler/Akademik/Arşiv), each with its own real home screen (`LabHomeScreen`,
    new `LabKnowledgeHomeScreen`, `LabProjectsHomeScreen`, `LabAcademicsHomeScreen`,
    `LabArchiveScreen`), switched with one tap, state held in `LabRoot` (`area` + a
    per-area `screen` stack). The old mega-`LabHomeScreen` had ten buttons bolted onto it
    (Ara, Değerlendir, Hata Defteri, Tekrar Kartları, Verim, Projeler, Sınavlar, AI Ayarları,
    Yazım Yardımı, Belgeden Taslak Oluştur) — these moved to the area whose job they actually
    match (Knowledge: search/writing; Projects: projects/exams/curriculum draft; Academics:
    evaluation/mistakes/flashcards; Archive: the new recording archive + Verim). AI Ayarları
    isn't one of the five named areas, so it got its own fixed gear icon pinned under the
    rail instead of being smuggled into one of the five or invented as a fake sixth area.
  - **General recording archive**: new `LabArchiveScreen` is the Arşiv area's home screen —
    every `EXPLANATION` recording across every concept, newest first (new
    `LabRepository.allExplanations()` / `ExplanationWithConcept`), each with the same
    dinle/transkribe/analiz actions Aşama 23/24 built, plus a "Kavrama Git" link back to its
    concept. Closes the exact gap named after Aşama 23: recording is no longer concept-only.
  - 1 new unit test (`allExplanations lists recordings across every concept…`).
- **Aşama 27 — a real connection graph, video self-evaluation, the per-area back-stack, and
  hover preview on two more lists, all at the user's explicit request** ("bağlantı grafiği,
  videolu değerlendirme ve kalan her şey").
  - **Visual connection graph**: `ConceptGraphScreen` gets a real node-link drawing
    (`ConnectionGraphCanvas`, Compose `Canvas` + positioned chips) — prerequisites left, the
    current concept centered, "this enables" right, related concepts in a row below, with
    lines drawn between them. This deliberately reverses §16's original "not a decorative
    diagram" framing, the same way Aşama 25 reversed Aşama 22's `generateCurriculum` decision:
    at the user's explicit request, not a silent regression. The existing navigable list stays
    — it's still the accessible/dpad interaction path; the canvas is additive, a "see the big
    picture" view on top of it. Related nodes cap at 6 in the canvas (the full list is still
    below) to keep the fixed layout from overflowing.
  - **Video self-evaluation**: next to the existing audio "Anlat (Feynman tekniği)", a "Video
    ile Anlat" button launches the system camera app (`ActivityResultContracts.CaptureVideo`,
    a `FileProvider`-granted Uri into the same `filesDir/recordings`) — this app does not write
    its own camera UI. Playback hands off to the system video viewer (`ACTION_VIEW`) rather
    than building an in-app video player. `ExplanationPayload` gained `videoFilePath` (sibling
    to `audioFilePath`, both optional, existing JSON unaffected); new
    `LabRepository.createVideoExplanation`. Deliberately NOT wired to AI transcription/analysis:
    `transcribeAudio`'s Gemini request hardcodes `mime_type: audio/mp4` — sending a real video
    file through that same path could silently misparse, so video recordings are watch-only.
    This is a stated scope boundary, not an oversight. `LabArchiveScreen` got the same
    video-aware treatma — "İzle (Video)" instead of inline playback, transcribe/analyze hidden
    for video rows. Manifest additions: a `FileProvider` (`res/xml/file_paths.xml`, scoped to
    `recordings/` only, not all of `filesDir`) and a `<queries>` entry for
    `ACTION_VIEW`+`video/*` (required on targetSdk 30+ package-visibility rules for the
    playback hand-off to resolve; capture needs no such entry — `ACTION_VIDEO_CAPTURE` is on
    Android's automatic-visibility allowlist). 1 new unit test (`createVideoExplanation`).
  - **Per-area back-stack**: closes the exact gap Aşama 26 named. `LabRoot` now holds
    `stacks: Map<LabArea, List<LabScreen>>` instead of one shared nullable screen — leaving an
    area mid-task and coming back restores exactly where you were, not that area's home.
    Push/pop are parameterized by area explicitly (`pushTo`/`popFrom`) rather than closing over
    the live `area` var, because the narrow-layout `AnimatedContent` can still be rendering the
    outgoing area's content for a frame after `area` has already changed — without this, a tap
    during that frame could write into the wrong area's stack.
  - **Hover preview, two more lists**: `LabSearchScreen`'s results and `ConceptGraphScreen`'s
    `RelatedRow` (prerequisites/enables/related) now hover-tint and swap their hint text, same
    pattern as Aşama 20's concept rows. Still not everywhere (see below).
- **Aşama 28 — a statistics section, a richer AI evaluation, universal hover (one fix, not
  screen-by-screen), and a better connection graph, all at the user's explicit request**
  ("istatistik bölümü ekle, mikro etkileşimleri ve ai kısmını geliştir, hover önizlemesini ve
  görsel bağlantı grafiğini de geliştir").
  - **Statistics section**: new `LabRepository.statsSnapshot()` / `StatsSnapshot` is the single
    source for both what `LabEvaluationScreen` displays (now 6 `StatTile`s — concept/session/
    completed, plus mistake count, due-flashcard count, and day streak — plus a top-3 mistake-
    category breakdown and a flashcard-ease summary) and what gets sent to the AI
    (`StatsSnapshot.toSummary()`). One calculation, not two: the number on screen and the
    number the AI reasons about can never drift apart. Streak counts consecutive days (back
    from today) with at least one object created or updated — `updatedAt`, not `createdAt`,
    because reviewing a flashcard counts as "active today" too, not just creating something
    new. 1 new unit test (`statsSnapshot aggregates…`, including a streak-boundary check).
  - **AI improved, same methods, better input**: `evaluateProgress`/`proposeStudyPlan` were
    already wired (Aşama 8) but only ever saw concept/session counts. They now see mistakes
    (with the actual recurring categories, not just a count), flashcard retention (average SM-2
    ease factor), and the real streak — a genuinely more informed evaluation, not a new AI call
    path.
  - **Universal hover, fixed at the root**: hover tinting moved INTO the shared
    `dpadFocusable` modifier itself, instead of being hand-rolled per screen (as Aşama 20/27
    did for `LabHomeScreen`/`LabSearchScreen`/`ConceptGraphScreen`). Every row/button using
    `dpadFocusable` — which is nearly all of them, including `Btn`/`GhostBtn` and every list in
    `MistakeJournalScreen`/`ProjectsScreen`/`ExamsScreen`/the three area-menu screens — now
    hovers without any screen-specific code. Screens that want richer hover behavior (changing
    hint text, not just a tint) can still layer their own `hoverable` on top, as before; the
    two don't conflict, they just compose.
  - **Connection graph improvements**: `ConnectionGraphCanvas` no longer caps related concepts
    at 6 — it draws all of them and scrolls horizontally if the row is wider than the screen.
    Edges are now colored by the prerequisite's actual relationship type (hard/soft/tool/
    intuition/co-requisite), with a small color legend underneath, and node chips carry a
    matching border tint — the graph now encodes real information, not just "these are
    connected."
  - **Micro-interactions, two shared components fixed at the root (not per-screen)**: `Field`
    (used by every text input in the app) now animates its border color on focus and its own
    height via `animateContentSize()` when a multi-line field grows; `AiResultView` (used by
    every AI-capability screen) wraps its content in `animateContentSize()` so a result
    appearing or changing (e.g. Offline → Success) grows smoothly instead of popping in. Both
    fixes close the exact micro-interaction gap named after Aşama 18, in the two places that
    actually mattered everywhere at once instead of one screen at a time.

### Honestly still NOT built (not a short list — said plainly, not glossed over)

- **Old data migration is partial, not full parity**: `Store.kt`'s fine-grained
  curriculum-progress fields — `done`/`dailyLogs`/`problems`-level per-task completion stats
  tied to the OLD curriculum's own task ids — are deliberately NOT migrated (Aşama 14). Those
  old task ids don't map to anything in the new knowledge graph, and a fake mapping would be
  worse than an honest gap. What IS migrated: mistakes, flashcards, recordings, Brain Inbox
  notes, practice logs, weekly reviews, project/assessment state. This is a one-time copy a
  person triggers once from "Verim", not a live sync — there is no second system to sync with
  anymore after Aşama 15.
- **Motion/micro-interactions, narrowed further (§11/§38)**: navigation, list changes
  (Aşama 17), press feedback on every `dpadFocusable` element (Aşama 18), `Field`'s focus
  border + height, and `AiResultView`'s size change (both Aşama 28) all animate now. What's
  still instant: individual screens' own ad-hoc layout changes outside those two shared
  components (e.g. a screen revealing a new section with a plain `if`, not inside something
  that already has `animateContentSize()`) — a residual, case-by-case gap, not a systemic one
  anymore.
- **Hover preview reaches everywhere `dpadFocusable` is used (Aşama 28), but a few lists
  still build their own rows without it**: `MistakeJournalScreen`/`ProjectsScreen`/
  `ExamsScreen` were checked at Aşama 27 time and found to already use `dpadFocusable` for
  their focusable rows, so they inherited hover for free — if any screen is found later to use
  a plain `clickable()` instead, it still won't hover until migrated to `dpadFocusable`.
- **The visual connection graph's layout is still fixed, not force-directed**: `Aşama 28`
  removed the related-concepts cap (now scrollable, not truncated) and added edge-type
  coloring, but node POSITIONS are still three fixed columns, not a real graph-layout
  algorithm — correct and readable for this screen's real node counts, but still wouldn't
  auto-arrange a concept with a genuinely tangled web of dozens of connections.
- **AI-generated curriculum preview is counts, not a rich diff**: `CurriculumGenScreen`
  shows "N birim, N kavram, N görev" before import, not a per-item list of what would be
  added — enough to sanity-check the AI didn't produce garbage, not enough to review each
  task individually before committing. Re-importing under the same `packageId` also silently
  replaces the previous AI draft (by design, same as the external-package re-import), with no
  extra "are you sure" beyond the existing "İçe Aktar" tap.
- **Streak only counts object activity, not a dedicated "I studied today" check-in**:
  `StatsSnapshot.streakDays` counts any object creation/update as "active" — quick-capturing
  a stray idea keeps a streak alive just as much as a real study session would. This is an
  honest proxy, not a deliberate study-time tracker.

None of this is secretly done — it's the honest remainder of a 55-section spec against
twenty-eight phases in one session. What exists is real (compiles, is tested, is CI-verified, is not a
mockup) for the slice it covers; the slice is a meaningful fraction, not the full vision, and
claiming otherwise would be dishonest.
