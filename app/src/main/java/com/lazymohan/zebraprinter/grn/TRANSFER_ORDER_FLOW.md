
## Flow overview

* User enters a **Transfer Order (TO) number** → we fetch the TO header + expected lines.
* For each expected line we **prefill one “Section #1”** (qty/lot/expiry) from Shipment & Lot APIs.
* We **do not** auto-create multiple sections from multiple shipment lines (UI still supports adding sections manually).
* User edits qty/lot, can add/remove lines & sections (we restore values if re-added).
* On **Review & Submit**, we build a **ReceiptRequestTo** payload and call Fusion’s **receivingReceiptRequests**.
* We show a **summary** with success/errors; when possible we also fetch **processingErrors** for richer diagnostics.

---

## Key pieces

### State & Models

* `ToUiState`: the screen state (header, lines, inputs, progress, step).
* `ToLineInput`: one selected TO line the user is receiving, with **sections**.
* `ToLineSection`: a single qty/lot/expiry entry (we default to only **Section #1**).
* Steps: `ENTER → RECEIVE → REVIEW → SUMMARY`.

### Main classes

* `ToViewModel`: orchestrates data loading, prefill, edits, and submit.
* `GrnRepository`: wraps Fusion REST APIs.
* `FusionApi`: Retrofit interface for Fusion endpoints.
* Compose UI: `ToAndReceiveCard`, `ToLineCard`, `SectionRow`.

---

## Screens & UX

1. **TO (Enter)**
   User types a TO number.

2. **Receive**

    * Header card: business unit, status, interface status.
    * Lines card:

        * “Add Item” opens a dialog listing **expected shipment lines** not yet added.
        * Each added line renders as its own **line card** (description, item, UOM, subinventory, TO line #, Shipment line #).
        * Inside the line card:

            * **Section #1** fields: Quantity, Lot, Expiry (expiry is read-only and fetched by lot).
            * “+ Add Section” is available (kept for power users), but **we don’t auto-add** sections from shipment lines.

3. **Review**
   Enabled when **any quantity > 0**. Shows a confirmation; proceeds to submit.

4. **Done (Summary)**
   Shows **ReturnStatus**, message, and any fetched **processingErrors**.

---

## Endpoints used (Fusion REST)

* **Step 1: Expected lines**
  `GET /receivingReceiptExpectedShipmentLines?q=OrganizationCode={org};TransferOrderNumber={to}`
  → Drives header + “Add Item” list.

* **Step 2: Shipment lines (for prefill)**
  `GET /shipmentLines?q=Order={to};Item="{item}";RequestedQuantity={orderedQty}`

  > We pass the **actual ordered quantity** from Step-1, **not** the literal `OrderedQuantity`.

* **Step 3: Lot expiry**
  `GET /inventoryItemLots?q=LotNumber="{lot}";OrganizationCode={org}`
  → Populates expiry for the section (read-only).

* **Submit receipt**
  `POST /receivingReceiptRequests` with `ReceiptRequestTo`.

* **Processing errors (optional detail)**
  `GET /receivingReceiptRequests/{headerInterfaceId}/child/lines/{interfaceTransactionId}/child/processingErrors`

---

## Flow (step-by-step)

### 1) Enter TO → `fetchTo()`

* Validates TO number.
* Calls `FetchToBundleUseCase` (returns header, expected lines, optional shipment header).
* On success, calls **`buildInputsWithShipments()`** to prefill line inputs.

### 2) Prefill inputs → `buildInputsWithShipments()`

For each **expected line**:

1. Call `repo.getShipmentLinesForOrderAndItem(toNumber, line.itemNumber, requestedQuantity = line.quantity)`.

    * This uses `shipmentLines` **with the ordered quantity** from Step-1 (`RequestedQuantity={line.quantity}`).
2. Choose **the first shipment** (if any) and create **only Section #1**:

    * `qty`: default from `shippedQuantity` (rounded to `Int`, or 0)
    * `lot`: from shipment `lotNumber`
3. If lot is present, fetch **expiry** via `repo.getLotExpiry(lot, "KDH")`.

> We intentionally **do not** auto-create multiple sections for multiple shipments. The UI still lets users add them manually via “+ Add Section”.

### 3) Edit lines & sections (UI)

* Users can **add/remove lines** and **add/remove sections**.
* **Value retention**:

    * When removing a **line**, we cache its last `ToLineInput` in `removedLineCache`.
    * When removing a **section**, we cache it in `removedSectionCache`.
    * When re-adding a line/section, we **restore** the cached values if available.
* Updating **lot** triggers an expiry refresh.

### 4) Review & Submit

* `canReview()` ensures header present and **total qty > 0**.
* `submitReceipt()`:

    1. Build **selected** lines (ignore sections with qty ≤ 0).
    2. Build request via **`buildToReceiptRequest()`**.
    3. Log JSON payload.
    4. Guard: if `shipmentNumber` missing → show error.
    5. `createReceipt()`; on success:

        * If `HeaderInterfaceId` + first `InterfaceTransactionId` exist, fetch **processingErrors** to enrich output.
    6. Move to **SUMMARY** with status + messages.

---

## How the request is built

### `buildToReceiptRequest()`

* **Header fields**:

    * `fromOrganizationCode` / `organizationCode`: inferred from Step-1 lines (first occurrence).
    * `employeeId`: `appPref.personId`.
    * `shipmentNumber`: first available `intransitShipmentNumber` across lines.

* **Lines** (`ReceiptLineTo`) — created from **selected** `ToLineSection`s:

    * `documentNumber`: line’s `intransitShipmentNumber`
    * `documentLineNumber`: line’s `shipmentLineNumber ?: 1`
    * `itemNumber`, `organizationCode`, `unitOfMeasure`, `subinventory`
    * `transferOrderHeaderId`, `transferOrderLineId`
    * `quantity`: section qty
    * `lotItemLots`: present when section `lot` is non-blank → includes `{lotNumber, transactionQuantity, lotExpirationDate}`
    * *(Locator omitted in prod) - when testing in prod found locator is not required for prod transaction*

---

## Important helper logic

### `stableLineId(headerId, line)`

Creates a stable, unique ID for each line so UI state can reliably track it:

1. If `documentLineId` exists → `(headerId << 32) + documentLineId`
2. Else if `transferOrderLineId` exists → use it
3. Else fallback → `(headerId << 20) + lineNumber`

### Caching removed values

* `removedLineCache: MutableMap<Long, ToLineInput>`
  Restores qty/lot/expiry if a line is re-added.
* `removedSectionCache: MutableMap<Pair<Long, Int>, ToLineSection>`
  Restores a specific section (by lineId + section index).

---

## Example (2 lines, single section each)

**Step-1** returns two expected lines (same item, different shipment line #s).
We show **two line cards**:

* **Line A (PH12988, TO line #16, Shipment line #13)**

    * Section #1: qty = 1000, lot = Y020731, expiry = 2026-07-01
* **Line B (PH12988, TO line #16, Shipment line #14)**

    * Section #1: qty = 611, lot = …, expiry = …

On submit, we send **two `ReceiptLineTo` entries** (one per **line card × Section #1**).
If user adds a **Section #2** to a line, that becomes an additional `ReceiptLineTo`.

---

## Error handling & messages

* Missing TO number → inline error in the ENTER step.
* **No `shipmentNumber`** → block submit with a clear message.
* API failures → surfaced as `submitError`, with `progress` set to `FAILED`.
* When possible, `processingErrors` are fetched and shown in SUMMARY.

---

## Where to look in code

| Area                                    | File / Function                                                              |
| --------------------------------------- | ---------------------------------------------------------------------------- |
| UI screens                              | `ToAndReceiveCard`, `ToLineCard`, `SectionRow`                               |
| State & flow                            | `ToViewModel`                                                                |
| Prefill logic                           | `ToViewModel.buildInputsWithShipments()`                                     |
| Submit mapping                          | `ToViewModel.buildToReceiptRequest()`                                        |
| API wrappers                            | `GrnRepository` & `FusionApi`                                                |
| Shipment query (with RequestedQuantity) | `GrnRepository.getShipmentLinesForOrderAndItem(to, item, requestedQuantity)` |

---

## Notes & gotchas

* **RequestedQuantity filter**: we now pass **the actual Step-1 ordered quantity** into `/shipmentLines` (`RequestedQuantity={line.quantity}`), not the text `OrderedQuantity`.
* Expiry is refreshed **after** lot changes (async).
* If you change how stable IDs are built, ensure **caches** and **UI reuse** remain correct.
* Locator is intentionally **omitted** for prod.

---

## Extending the flow

* To change the prefill policy (e.g., auto-create multiple sections), adjust `buildInputsWithShipments()`.
* To add validation (e.g., prevent over-receiving), intercept in `updateSectionQty()` or `canReview()`.

---

*End of file.*
