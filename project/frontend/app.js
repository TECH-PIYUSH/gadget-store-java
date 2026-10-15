/* ===========================================================================
   GadgetStore frontend logic (vanilla JavaScript).
   The REAL inventory / order / stock logic runs in the JAVA backend.
   This file only renders data and sends HTTP requests to the backend.
   =========================================================================== */

// Where is the Java backend? When the page is opened directly as a file we use
// the fixed local address; otherwise we call the same origin that served the page.
const API_BASE =
  location.protocol === "file:" ? "http://localhost:8080/api" : location.origin + "/api";

// Simple in-memory copy of the gadget list we last received from the backend
// (used only for filtering/display; the source of truth is always the backend).
let gadgets = [];

// ---------- helpers ----------
function $(sel) { return document.querySelector(sel); }
function esc(s) {
  return String(s == null ? "" : s)
    .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}
// Formats a number as Indian Rupees, e.g. 71249.05 -> "₹71,249.05".
function formatINR(n) {
  const hasPaise = Math.round(n * 100) % 100 !== 0;
  return "₹" + Number(n).toLocaleString("en-IN", {
    minimumFractionDigits: hasPaise ? 2 : 0,
    maximumFractionDigits: 2,
  });
}

// ---------- navigation (single page app) ----------
function showSection(name) {
  document.querySelectorAll(".section").forEach((s) => s.classList.remove("active"));
  document.querySelectorAll(".nav-link").forEach((l) => l.classList.remove("active"));
  $("#section-" + name).classList.add("active");
  const link = document.querySelector('.nav-link[data-section="' + name + '"]');
  if (link) link.classList.add("active");
  window.scrollTo({ top: 0, behavior: "smooth" });
}
document.querySelectorAll(".nav-link").forEach((link) => {
  link.addEventListener("click", (e) => {
    e.preventDefault();
    showSection(link.dataset.section);
  });
});

// ---------- CATALOG ----------
async function loadGadgets() {
  try {
    const res = await fetch(API_BASE + "/gadgets");
    gadgets = await res.json();
    renderCatalog();
    fillOrderGadgetSelect();
  } catch (err) {
    $("#catalog-grid").innerHTML =
      '<p class="message err">Could not reach the Java backend. Make sure it is running.</p>';
  }
}

function renderCatalog() {
  const term = $("#search").value.trim().toLowerCase();
  const category = $("#category-filter").value;
  const grid = $("#catalog-grid");

  const list = gadgets.filter((g) => {
    const matchesTerm =
      !term || g.name.toLowerCase().includes(term) || g.brand.toLowerCase().includes(term);
    const matchesCat = category === "All" || g.category === category;
    return matchesTerm && matchesCat;
  });

  $("#catalog-empty").style.display = list.length === 0 ? "block" : "none";

  grid.innerHTML = list.map(cardHtml).join("");

  // Wire up the Buy buttons after rendering.
  grid.querySelectorAll("[data-buy]").forEach((btn) => {
    btn.addEventListener("click", () => startOrder(btn.dataset.buy));
  });
}

function cardHtml(g) {
  const catClass =
    g.category === "Mobile" ? "badge-mobile" :
    g.category === "Laptop" ? "badge-laptop" : "badge-smartwatch";
  const stockBadge = g.stock > 0
    ? '<span class="badge badge-stock" data-testid="stock-badge-' + esc(g.serialNumber) + '">' + g.stock + " in stock</span>"
    : '<span class="badge badge-out" data-testid="stock-badge-' + esc(g.serialNumber) + '">Out of stock</span>';
  const off = g.discountPercent > 0 ? '<span class="price-off">' + g.discountPercent + "% off</span>" : "";
  const buyDisabled = g.stock > 0 ? "" : "disabled";

  return (
    '<article class="card product-card" data-testid="product-card-' + esc(g.serialNumber) + '">' +
      '<div class="card-top">' +
        '<span class="badge ' + catClass + '">' + esc(g.category) + "</span>" +
        stockBadge +
      "</div>" +
      '<h3 class="product-name">' + esc(g.name) + "</h3>" +
      '<p class="product-sub">' + esc(g.brand) + " · " + esc(g.specifications) + "</p>" +
      '<p class="product-meta">' + g.warrantyMonths + " months warranty · Serial " + esc(g.serialNumber) + "</p>" +
      '<div class="price-row">' +
        '<span class="price-now">' + formatINR(g.sellingPrice) + "</span>" +
        '<span class="price-old">' + formatINR(g.originalPrice) + "</span>" +
        off +
      "</div>" +
      '<button class="btn btn-primary btn-buy" ' + buyDisabled +
        ' data-buy="' + esc(g.serialNumber) + '" data-testid="buy-' + esc(g.serialNumber) + '">Buy</button>' +
    "</article>"
  );
}

$("#search").addEventListener("input", renderCatalog);
$("#category-filter").addEventListener("change", renderCatalog);

// ---------- ADD GADGET ----------
$("#add-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const msg = $("#add-message");
  const form = e.target;
  const payload = {
    name: form.name.value.trim(),
    brand: form.brand.value.trim(),
    category: form.category.value,
    serialNumber: form.serialNumber.value.trim(),
    specifications: form.specifications.value.trim(),
    sellingPrice: form.sellingPrice.value,
    originalPrice: form.originalPrice.value,
    stock: form.stock.value,
    warrantyMonths: form.warrantyMonths.value,
  };
  try {
    const res = await fetch(API_BASE + "/gadgets", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    const data = await res.json();
    if (!res.ok) {
      msg.textContent = data.error || "Could not add the gadget.";
      msg.className = "message err";
      return;
    }
    msg.textContent = "Gadget '" + data.name + "' added successfully.";
    msg.className = "message ok";
    form.reset();
    await loadGadgets(); // refresh catalog so the new product appears immediately
  } catch (err) {
    msg.textContent = "Could not reach the Java backend.";
    msg.className = "message err";
  }
});

// ---------- PLACE ORDER ----------
function fillOrderGadgetSelect() {
  const select = $("#order-gadget");
  const current = select.value;
  select.innerHTML = gadgets
    .map((g) => {
      const label = g.name + " — " + formatINR(g.sellingPrice) + (g.stock > 0 ? " (" + g.stock + " in stock)" : " (Out of stock)");
      return '<option value="' + esc(g.serialNumber) + '"' + (g.stock > 0 ? "" : " disabled") + ">" + esc(label) + "</option>";
    })
    .join("");
  if (current && gadgets.some((g) => g.serialNumber === current)) select.value = current;
  updateTotal();
}

function selectedGadget() {
  return gadgets.find((g) => g.serialNumber === $("#order-gadget").value);
}

function updateTotal() {
  const g = selectedGadget();
  const qty = parseInt($("#order-qty").value, 10) || 0;
  $("#order-total").textContent = g ? formatINR(g.sellingPrice * qty) : "₹0";
}

// Called when a Buy button on a catalog card is clicked.
function startOrder(serial) {
  showSection("order");
  $("#order-confirmation").style.display = "none";
  $("#order-message").textContent = "";
  const select = $("#order-gadget");
  select.value = serial;
  $("#order-qty").value = 1;
  updateTotal();
}

$("#order-gadget").addEventListener("change", updateTotal);
$("#order-qty").addEventListener("input", updateTotal);

$("#order-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const msg = $("#order-message");
  const form = e.target;
  const payload = {
    fullName: form.fullName.value.trim(),
    mobile: form.mobile.value.trim(),
    email: form.email.value.trim(),
    address: form.address.value.trim(),
    city: form.city.value.trim(),
    pinCode: form.pinCode.value.trim(),
    serialNumber: form.serialNumber.value,
    quantity: form.quantity.value,
  };
  try {
    const res = await fetch(API_BASE + "/orders", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    const data = await res.json();
    if (!res.ok) {
      msg.textContent = data.error || "Order could not be placed.";
      msg.className = "message err";
      $("#order-confirmation").style.display = "none";
      return;
    }
    msg.textContent = "";
    showConfirmation(data);
    form.reset();
    $("#order-qty").value = 1;
    await loadGadgets();  // refresh catalog stock
    await loadHistory();  // refresh history
  } catch (err) {
    msg.textContent = "Could not reach the Java backend.";
    msg.className = "message err";
  }
});

function showConfirmation(o) {
  const box = $("#order-confirmation");
  box.style.display = "block";
  box.innerHTML =
    "<h3>Order placed successfully!</h3>" +
    row("Order ID", o.orderId) +
    row("Customer", o.customerName) +
    row("Gadget", o.gadgetName) +
    row("Quantity", o.quantity) +
    row("Total amount", formatINR(o.totalAmount)) +
    row("Payment method", o.paymentMethod);
  box.scrollIntoView({ behavior: "smooth" });
}
function row(label, value) {
  return '<div class="row"><span>' + esc(label) + "</span><span><strong>" + esc(value) + "</strong></span></div>";
}

// ---------- HISTORY ----------
async function loadHistory() {
  try {
    const res = await fetch(API_BASE + "/orders");
    const orders = await res.json();
    const body = $("#history-body");
    $("#history-empty").style.display = orders.length === 0 ? "block" : "none";
    body.innerHTML = orders
      .map(
        (o) =>
          "<tr>" +
          "<td>" + esc(o.orderId) + "</td>" +
          "<td>" + esc(o.customerName) + "</td>" +
          "<td>" + esc(o.mobile) + "</td>" +
          "<td>" + esc(o.gadgetName) + "</td>" +
          "<td>" + o.quantity + "</td>" +
          "<td>" + formatINR(o.totalAmount) + "</td>" +
          "<td>" + esc(o.paymentMethod) + "</td>" +
          "<td>" + esc(o.address) + ", " + esc(o.city) + " - " + esc(o.pinCode) + "</td>" +
          "<td>" + esc(o.orderDateTime) + "</td>" +
          '<td><span class="status-pill">' + esc(o.status) + "</span></td>" +
          "</tr>"
      )
      .join("");
  } catch (err) {
    $("#history-body").innerHTML =
      '<tr><td colspan="10" class="message err">Could not reach the Java backend.</td></tr>';
  }
}

// ---------- start ----------
loadGadgets();
loadHistory();
