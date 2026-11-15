
"use strict";

const API = "";

// USER ROLES
const demoRoles = {
    student1: "STUDENT",
    manager1: "FACILITY_MANAGER",
    tech1: "TECHNICIAN",
    approver1: "APPROVER",
    admin1: "ADMIN"
};

// SESSION
let authHeaders = {
    Authorization: sessionStorage.getItem("campusopsAuth") || ""
};

let currentUsername =
    sessionStorage.getItem("campusopsUsername") || "";

let currentRole =
    demoRoles[currentUsername.toLowerCase()] || "";

// API REQUEST
async function apiRequest(url, options = {}) {
    const response = await fetch(API + url, {
        ...options,
        headers: {
            ...authHeaders,
            ...(options.headers || {}),
            "Content-Type": "application/json"
        }
    });

    const text = await response.text();

    let data = null;

    try {
        data = text ? JSON.parse(text) : null;
    } catch {
        data = text;
    }

    if (!response.ok) {
        throw new Error(
            typeof data === "string"
                ? data
                : data?.message || `Request failed (${response.status})`
        );
    }

    return data;
}

// HTML ESCAPING
function escapeHTML(value) {
    return String(value ?? "").replace(/[&<>"']/g, char => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[char]);
}

// NOTIFICATIONS
function showMessage(message) {
    const box = document.getElementById("notification");

    if (!box) {
        alert(message);
        return;
    }

    box.textContent = message;
    box.classList.add("show");

    setTimeout(() => {
        box.classList.remove("show");
    }, 3500);
}

// ROLE HELPERS
function isManager() {
    return ["FACILITY_MANAGER", "ADMIN"].includes(currentRole);
}

function isApprover() {
    return ["APPROVER", "ADMIN"].includes(currentRole);
}

// SHOW DASHBOARD
function showDashboard() {
    document.getElementById("loginPage").classList.add("hidden");
    document.getElementById("dashboardPage").classList.remove("hidden");

    document.getElementById("userInfo").textContent =
        `${currentUsername} | ${currentRole}`;

    document.getElementById("bookingFormContainer").classList.toggle(
        "hidden",
        !["STUDENT", "FACILITY_MANAGER", "ADMIN"].includes(currentRole)
    );

    document.getElementById("ticketFormContainer").classList.toggle(
        "hidden",
        !["STUDENT", "FACILITY_MANAGER", "ADMIN"].includes(currentRole)
    );

    document.querySelector('[data-section="audit"]').classList.toggle(
        "hidden",
        !["FACILITY_MANAGER", "APPROVER", "ADMIN"].includes(currentRole)
    );
}

// LOGIN
document.getElementById("loginForm").addEventListener("submit", async event => {
    event.preventDefault();

    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;
    const message = document.getElementById("loginMessage");

    message.textContent = "Checking credentials...";

    try {
        const credentials =
            "Basic " + btoa(`${username}:${password}`);

        authHeaders = {
            Authorization: credentials
        };

        // Verify login with backend
        await apiRequest("/api/facilities");

        currentUsername = username;
        currentRole = demoRoles[username.toLowerCase()] || "USER";

        sessionStorage.setItem("campusopsAuth", credentials);
        sessionStorage.setItem("campusopsUsername", username);

        showDashboard();

        message.textContent = "";

        document.getElementById("loginForm").reset();

        showMessage("Login successful!");

        await loadDashboard();

    } catch (error) {
        authHeaders = {};
        currentUsername = "";
        currentRole = "";

        sessionStorage.removeItem("campusopsAuth");
        sessionStorage.removeItem("campusopsUsername");

        message.textContent = error.message;
    }
});

// NAVIGATION
document.querySelectorAll("[data-section]").forEach(button => {
    button.addEventListener("click", async () => {
        const section = button.dataset.section;

        document.querySelectorAll(".content-section").forEach(item => {
            item.classList.add("hidden");
        });

        document.getElementById(section + "Section").classList.remove("hidden");

        const titles = {
            overview: "Dashboard",
            bookings: "Facility Bookings",
            maintenance: "Maintenance Management",
            audit: "Audit History"
        };

        document.getElementById("pageTitle").textContent =
            titles[section] || "Dashboard";

        try {
            if (section === "bookings") await loadBookings();
            if (section === "maintenance") await loadTickets();
            if (section === "audit") await loadAudit();
        } catch (error) {
            showMessage(error.message);
        }
    });
});

// DASHBOARD
async function loadDashboard() {
    try {
        const facilities = await apiRequest("/api/facilities");

        document.getElementById("facilityCount").textContent =
            facilities.length;

        const options = facilities.map(facility =>
            `<option value="${facility.id}">${escapeHTML(facility.name)}</option>`
        ).join("");

        document.getElementById("facilityId").innerHTML =
            '<option value="">Choose facility</option>' + options;

        document.getElementById("ticketFacilityId").innerHTML =
            '<option value="">Choose facility</option>' + options;

    } catch (error) {
        showMessage("Could not load facilities: " + error.message);
    }

    try {
        await loadBookings();
    } catch (error) {
        showMessage("Could not load bookings: " + error.message);
    }

    try {
        await loadTickets();
    } catch (error) {
        showMessage("Could not load tickets: " + error.message);
    }
}

// BOOKINGS
async function loadBookings() {
    let bookings;

    if (isManager()) {
        bookings = await apiRequest("/api/bookings");
    } else {
        bookings = await apiRequest("/api/bookings/my");
    }

    document.getElementById("bookingCount").textContent =
        bookings.length;

    const container = document.getElementById("bookingList");

    if (!bookings.length) {
        container.innerHTML = "<p>No booking records found.</p>";
        return;
    }

    container.innerHTML = bookings.map(booking => {
        let actions = "";

        if (isManager() && booking.status === "PENDING") {
            actions += `
                <button onclick="bookingAction(${booking.id}, 'approve')">Approve</button>
                <button onclick="bookingAction(${booking.id}, 'reject')">Reject</button>
            `;
        }

        if (isManager() && booking.status === "APPROVED") {
            actions += `
                <button onclick="bookingAction(${booking.id}, 'complete')">Mark Completed</button>
            `;
        }

        if (
            booking.status === "PENDING" &&
            booking.requestedBy?.username === currentUsername
        ) {
            actions += `
                <button onclick="bookingAction(${booking.id}, 'cancel')">Cancel</button>
            `;
        }

        return `
            <div class="record-card">
                <h3>Booking #${booking.id}</h3>
                <p><strong>Facility:</strong> ${escapeHTML(booking.facility?.name || booking.facility?.id)}</p>
                <p><strong>Purpose:</strong> ${escapeHTML(booking.purpose)}</p>
                <p><strong>Start:</strong> ${escapeHTML(booking.startTime)}</p>
                <p><strong>End:</strong> ${escapeHTML(booking.endTime)}</p>
                <span class="status">${escapeHTML(booking.status)}</span>
                <div>${actions}</div>
            </div>
        `;
    }).join("");
}

// CREATE BOOKING
document.getElementById("bookingForm").addEventListener("submit", async event => {
    event.preventDefault();

    try {
        const request = {
            facilityId: Number(document.getElementById("facilityId").value),
            startTime: document.getElementById("startTime").value,
            endTime: document.getElementById("endTime").value,
            purpose: document.getElementById("bookingPurpose").value,
            requestKey: crypto.randomUUID()
        };

        await apiRequest("/api/bookings", {
            method: "POST",
            body: JSON.stringify(request)
        });

        event.target.reset();

        await loadBookings();

        showMessage("Booking request submitted!");

    } catch (error) {
        showMessage(error.message);
    }
});

// BOOKING ACTIONS
async function bookingAction(id, action) {
    try {
        await apiRequest(`/api/bookings/${id}/${action}`, {
            method: "POST"
        });

        await loadBookings();

        showMessage(`Booking ${action} successful!`);

    } catch (error) {
        showMessage(error.message);
    }
}

// MAINTENANCE TICKETS
async function loadTickets() {
    const container = document.getElementById("ticketList");

    if (currentRole === "STUDENT") {
        document.getElementById("ticketCount").textContent = "—";
        container.innerHTML =
            "<p>You can submit a maintenance ticket using the form.</p>";
        return;
    }

    let tickets = [];

    if (currentRole === "TECHNICIAN") {
        tickets = await apiRequest("/api/tickets/assigned");
    } else if (
        ["FACILITY_MANAGER", "APPROVER", "ADMIN"].includes(currentRole)
    ) {
        tickets = await apiRequest("/api/tickets");
    } else {
        document.getElementById("ticketCount").textContent = "0";
        container.innerHTML = "<p>No maintenance records available.</p>";
        return;
    }

    document.getElementById("ticketCount").textContent =
        tickets.length;

    if (!tickets.length) {
        container.innerHTML = "<p>No maintenance tickets found.</p>";
        return;
    }

    container.innerHTML = tickets.map(ticket => {
        let actions = "";

        if (isManager() && ticket.status === "OPEN") {
            actions += `
                <button onclick="ticketAction(${ticket.id}, 'triage')">Triage</button>
            `;
        }

        if (isManager() && ticket.status === "TRIAGED") {
            actions += `
                <button onclick="assignTicket(${ticket.id})">Assign Technician</button>
            `;
        }

        if (currentRole === "TECHNICIAN" && ticket.status === "ASSIGNED") {
            actions += `
                <button onclick="ticketNotes(${ticket.id}, 'inspect')">Inspect</button>
            `;
        }

        if (currentRole === "TECHNICIAN" && ticket.status === "INSPECTION") {
            actions += `
                <button onclick="ticketNotes(${ticket.id}, 'repair')">Submit Repair</button>
            `;
        }

        if (isApprover() && ticket.status === "AWAITING_APPROVAL") {
            actions += `
                <button onclick="ticketNotes(${ticket.id}, 'approve')">Approve</button>
                <button onclick="ticketNotes(${ticket.id}, 'reject')">Reject</button>
            `;
        }

        return `
            <div class="record-card">
                <h3>Ticket #${ticket.id}: ${escapeHTML(ticket.title)}</h3>
                <p>${escapeHTML(ticket.description)}</p>
                <p><strong>Facility:</strong> ${escapeHTML(ticket.facility?.name || ticket.facility?.id)}</p>
                <p><strong>Priority:</strong> ${escapeHTML(ticket.priority)}</p>
                <p><strong>Reason:</strong> ${escapeHTML(ticket.triageReason)}</p>
                <p><strong>Assigned Technician:</strong> ${escapeHTML(ticket.assignedTechnician?.username || "Not assigned")}</p>
                <span class="status">${escapeHTML(ticket.status)}</span>
                <div>${actions}</div>
            </div>
        `;
    }).join("");
}

// CREATE MAINTENANCE TICKET
document.getElementById("ticketForm").addEventListener("submit", async event => {
    event.preventDefault();

    try {
        const request = {
            facilityId: Number(document.getElementById("ticketFacilityId").value),
            title: document.getElementById("ticketTitle").value,
            description: document.getElementById("ticketDescription").value,
            requestKey: crypto.randomUUID()
        };

        await apiRequest("/api/tickets", {
            method: "POST",
            body: JSON.stringify(request)
        });

        event.target.reset();

        await loadTickets();

        showMessage("Maintenance ticket submitted!");

    } catch (error) {
        showMessage(error.message);
    }
});

// TICKET ACTIONS
async function ticketAction(id, action) {
    try {
        await apiRequest(`/api/tickets/${id}/${action}`, {
            method: "POST"
        });

        await loadTickets();

        showMessage("Ticket updated successfully!");

    } catch (error) {
        showMessage(error.message);
    }
}

// ASSIGN TECHNICIAN
async function assignTicket(id) {
    const technicianId = prompt(
        "Enter technician user ID (tech1 has ID 3):"
    );

    if (!technicianId) return;

    if (!Number.isInteger(Number(technicianId)) || Number(technicianId) <= 0) {
        showMessage("Enter a valid technician ID.");
        return;
    }

    try {
        await apiRequest(`/api/tickets/${id}/assign`, {
            method: "POST",
            body: JSON.stringify({
                technicianId: Number(technicianId)
            })
        });

        await loadTickets();

        showMessage("Technician assigned!");

    } catch (error) {
        showMessage(error.message);
    }
}

// INSPECTION, REPAIR AND APPROVAL NOTES
async function ticketNotes(id, action) {
    const notes = prompt("Enter notes:");

    if (!notes || !notes.trim()) return;

    try {
        await apiRequest(`/api/tickets/${id}/${action}`, {
            method: "POST",
            body: JSON.stringify({
                notes: notes.trim()
            })
        });

        await loadTickets();

        showMessage(`Ticket ${action} successful!`);

    } catch (error) {
        showMessage(error.message);
    }
}

// AUDIT HISTORY
async function loadAudit() {
    const records = await apiRequest("/api/audit");

    const container = document.getElementById("auditList");

    if (!records.length) {
        container.innerHTML = "<p>No audit records found.</p>";
        return;
    }

    container.innerHTML = records.map(record => `
        <div class="record-card">
            <h3>${escapeHTML(record.action)}</h3>
            <p><strong>Actor:</strong> ${escapeHTML(record.actor)}</p>
            <p><strong>Entity:</strong> ${escapeHTML(record.entityType)} #${escapeHTML(record.entityId)}</p>
            <p><strong>Details:</strong> ${escapeHTML(record.details)}</p>
            <p><strong>Time:</strong> ${escapeHTML(record.createdAt)}</p>
        </div>
    `).join("");
}

// RESTORE SESSION
async function restoreSession() {
    if (!authHeaders.Authorization || !currentUsername) {
        return;
    }

    try {
        await apiRequest("/api/facilities");

        currentRole =
            demoRoles[currentUsername.toLowerCase()] || "USER";

        showDashboard();

        await loadDashboard();

    } catch (error) {
        sessionStorage.removeItem("campusopsAuth");
        sessionStorage.removeItem("campusopsUsername");

        authHeaders = {};
        currentUsername = "";
        currentRole = "";
    }
}

// LOGOUT
document.getElementById("logoutButton").addEventListener("click", () => {
    sessionStorage.removeItem("campusopsAuth");
    sessionStorage.removeItem("campusopsUsername");

    authHeaders = {};
    currentUsername = "";
    currentRole = "";

    document.getElementById("dashboardPage").classList.add("hidden");
    document.getElementById("loginPage").classList.remove("hidden");

    document.getElementById("loginForm").reset();

    showMessage("Logged out successfully.");
});

// RESTORE SESSION ON PAGE LOAD
restoreSession();
