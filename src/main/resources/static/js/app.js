document.addEventListener("DOMContentLoaded", () => {
    const clockEl = document.getElementById("liveClock");
    if (clockEl) {
        const tz = clockEl.dataset.timezone || undefined;
        const updateClock = () => {
            try {
                const now = new Date();
                clockEl.textContent = now.toLocaleTimeString("es-DO", {
                    hour: "2-digit",
                    minute: "2-digit",
                    second: "2-digit",
                    hour12: false,
                    timeZone: tz
                });
            } catch (e) {
                clockEl.textContent = new Date().toLocaleTimeString();
            }
        };
        updateClock();
        setInterval(updateClock, 1000);
    }

    const modal = document.getElementById("adjustModal");
    if (modal) {
        const form = document.getElementById("adjustForm");
        const empLabel = document.getElementById("adjustEmployeeLabel");
        const inInput = document.getElementById("adjustCheckIn");
        const outInput = document.getElementById("adjustCheckOut");
        const notesInput = document.getElementById("adjustNotes");

        document.querySelectorAll("[data-open-adjust]").forEach((btn) => {
            btn.addEventListener("click", () => {
                const recordId = btn.dataset.recordId;
                const empName = btn.dataset.employeeName || "";
                const workDate = btn.dataset.workDate || "";
                const checkIn = btn.dataset.checkIn || "";
                const checkOut = btn.dataset.checkOut || "";

                form.action = `/attendance/${recordId}/adjust`;
                empLabel.textContent = `${empName} — ${workDate}`;
                inInput.value = checkIn.length >= 5 ? checkIn.substring(0, 5) : "";
                outInput.value = checkOut.length >= 5 ? checkOut.substring(0, 5) : "";
                notesInput.value = "";
                modal.classList.add("open");
            });
        });

        document.querySelectorAll("[data-close-modal]").forEach((btn) => {
            btn.addEventListener("click", () => modal.classList.remove("open"));
        });

        modal.addEventListener("click", (e) => {
            if (e.target === modal) {
                modal.classList.remove("open");
            }
        });
    }
});

function fillDemoCredentials(email, password) {
    const emailEl = document.getElementById("username");
    const passEl = document.getElementById("password");
    if (emailEl && passEl) {
        emailEl.value = email;
        passEl.value = password;
    }
}
