# GadgetStore — Electronic Gadgets Store Management System

A simple, fully working store management system built for a second-year B.Tech mini-project.

- **Frontend:** HTML, CSS and vanilla JavaScript (no framework).
- **Backend:** pure Java using the JDK's built-in HTTP server (`com.sun.net.httpserver`). No external libraries.
- **Communication:** the browser talks to Java over HTTP using JSON.
- **Storage:** in-memory Java collections for runtime + **CSV file handling** for persistence (no database).

---

## 1. Folder structure

```
project/
├── backend/                  <- all Java source files live here
│   ├── Gadget.java           (abstract parent class)
│   ├── MobileGadget.java     (extends Gadget)
│   ├── LaptopGadget.java     (extends Gadget)
│   ├── SmartwatchGadget.java (extends Gadget)
│   ├── Customer.java
│   ├── Order.java
│   ├── InventoryManager.java (Map + HashSet collections)
│   ├── StoreService.java     (validates + processes orders)
│   ├── FileManager.java      (ALL CSV file handling)
│   ├── OutOfStockException.java      (custom exception)
│   ├── DuplicateSerialException.java (custom exception)
│   ├── Json.java             (tiny JSON helper, no library)
│   ├── Main.java             (seeds data + starts the HTTP server)
│   └── data/                 (auto-created on first run)
│       ├── gadgets.csv
│       ├── customers.csv
│       └── orders.csv
└── frontend/
    ├── index.html
    ├── styles.css
    └── app.js
```

---

## 2. How to run on Windows

### Prerequisite
Install **Java JDK 17 or newer** and make sure `java` and `javac` work in Command Prompt:

```
java -version
javac -version
```
(If not found, install from https://adoptium.net and reopen Command Prompt.)

### Run the application (single command window)

```
cd project\backend
javac *.java
java Main
```

You will see:
```
GadgetStore backend is running.
Open your browser at:  http://localhost:8080
```

Now open **http://localhost:8080** in your browser. The Java server serves both the
website and the API, so there is nothing else to start.

> The frontend calls the backend at `http://localhost:8080/api`. Because the same
> Java server serves the web page too, there are no CORS problems. (CORS headers are
> still sent, so opening `frontend/index.html` directly also works.)

To stop the server press **Ctrl + C**.

---

## 3. REST API

| Method | Endpoint        | Purpose                      |
|--------|-----------------|------------------------------|
| GET    | `/api/gadgets`  | List all gadgets             |
| POST   | `/api/gadgets`  | Add a new gadget             |
| GET    | `/api/orders`   | Get successful order history |
| POST   | `/api/orders`   | Place a Cash-on-Delivery order |

Errors are returned as `{ "error": "message" }` with a proper HTTP status
(400 = bad input, 409 = duplicate serial).

---

## 4. The classes and the four OOP pillars

| Pillar | Where to show it in the viva |
|--------|------------------------------|
| **Abstraction** | `Gadget` is an **abstract class** with the abstract method `displaySpecifications()`. You cannot create a plain `Gadget`. |
| **Inheritance** | `MobileGadget`, `LaptopGadget`, `SmartwatchGadget` all **extend** `Gadget` and reuse its fields via `super(...)`. |
| **Polymorphism** | `displaySpecifications()` / `getCategory()` are **overridden** in each subclass. In `Gadget.toJson()` a `Gadget` reference calls the subclass version at runtime. The factory in `StoreService.createGadget()` returns different subclasses through one `Gadget` reference. |
| **Encapsulation** | All fields in `Gadget`, `Customer`, `Order` are **private**. Stock changes only through `reduceStock()`, which guarantees stock never goes negative. |

Also demonstrated: constructors, access modifiers (`private`/`public`),
`static final` constant (`StoreService.PAYMENT_METHOD`), the `final` serial field,
and object relationships (an `Order` **has-a** `Customer`).

### Generic collections (used in real logic, not just demos)
- `Map<String, Gadget>` (a `LinkedHashMap`) in `InventoryManager` — maps a unique serial number to its gadget. This is the "`Dictionary<String,Gadget>`" equivalent asked for; `LinkedHashMap` is used so the catalog keeps a tidy order.
- `HashSet<String>` in `InventoryManager` — guarantees serial numbers stay unique.
- `List<Order>` in `StoreService` — stores the history of successful orders.

### Custom exceptions
- `OutOfStockException` — thrown when a gadget is out of stock, the quantity is more than the stock, or the quantity is invalid.
- `DuplicateSerialException` — thrown when a gadget is added with a serial number that already exists.

Both are caught in `Main` and returned to the browser as readable error messages.

---

## 5. File handling (`FileManager`)

All file handling is inside **`FileManager.java`**, using `File`, `FileWriter`,
`FileReader` and `BufferedReader`, all opened with **try-with-resources** so they
close safely. `IOException` is caught and a clear message is printed.

CSV files created automatically inside `backend/data/`:

- **`gadgets.csv`** — rewritten whenever a gadget is added or stock changes.
- **`customers.csv`** — a new row is **appended** for every successful order.
- **`orders.csv`** — a new row is **appended** for every successful order (old rows are never overwritten).

Commas and quotes inside a field are handled correctly (the field is wrapped in
double quotes and inner quotes are doubled).

### What persists
Because of file handling, **gadgets you add, stock changes and order history
survive a restart**. When Java starts it loads `gadgets.csv` and `orders.csv`.
If the files are empty/missing, it seeds the 12 sample gadgets and saves them.

### Demonstrate for the viva
1. Add a gadget in the **Add Gadget** tab, then open `backend/data/gadgets.csv` — the new row is there.
2. Place a COD order, then open `backend/data/orders.csv` — the order is recorded.
3. Stop Java (Ctrl + C) and run `java Main` again — the saved products, updated stock and order history are restored.

> To start fresh, simply delete the `backend/data` folder (or the CSV files) and run again — the 12 samples will be re-seeded.

---

## 6. Manual tests

1. **Catalog loads** — all 12 products appear; search and the category dropdown filter the list.
2. **Successful COD order** — click **Buy**, fill customer details, place the order. You get an order ID and the stock drops by the quantity; the order shows in **History**.
3. **Insufficient stock** — try to order more units than available → you see an "out of stock / not enough stock" error, and stock/history do **not** change.
4. **Out-of-stock product** — Pixel 8 and Noise ColorFit start at 0 stock; their **Buy** buttons are disabled and show a red badge.
5. **Duplicate serial** — in **Add Gadget**, use an existing serial (e.g. `MOB001`) → you see a "serial number already exists" error and nothing is added.
6. **Validation** — empty fields, negative price/stock, a mobile that is not 10 digits, or a PIN that is not 6 digits are all rejected by the Java backend.
