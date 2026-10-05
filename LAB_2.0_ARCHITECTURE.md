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

API keys stay in `ApiKeyStore.kt`'s `EncryptedSharedPreferences` approach (KEEP — already
correct per §35).

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
