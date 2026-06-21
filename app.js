const header = document.querySelector("[data-site-header]");
const navToggle = document.querySelector("[data-nav-toggle]");
const siteNav = document.querySelector("[data-site-nav]");
const yearNode = document.querySelector("[data-year]");
const previewDialog = document.querySelector("[data-preview-dialog]");
const dialogImage = document.querySelector("[data-dialog-image]");
const dialogTitle = document.querySelector("[data-dialog-title]");
const dialogClose = document.querySelector("[data-dialog-close]");

if (yearNode) {
  yearNode.textContent = new Date().getFullYear();
}

const syncHeader = () => {
  if (!header || header.classList.contains("site-header--solid")) {
    return;
  }
  header.classList.toggle("is-scrolled", window.scrollY > 16);
};

syncHeader();
window.addEventListener("scroll", syncHeader, { passive: true });

if (navToggle && siteNav) {
  navToggle.addEventListener("click", () => {
    const nextState = navToggle.getAttribute("aria-expanded") !== "true";
    navToggle.setAttribute("aria-expanded", String(nextState));
    siteNav.classList.toggle("is-open", nextState);
  });

  siteNav.addEventListener("click", (event) => {
    if (event.target instanceof HTMLAnchorElement) {
      navToggle.setAttribute("aria-expanded", "false");
      siteNav.classList.remove("is-open");
    }
  });
}

document.querySelectorAll("[data-preview]").forEach((button) => {
  button.addEventListener("click", () => {
    const source = button.getAttribute("data-preview");
    const title = button.getAttribute("data-title") || "截图预览";

    if (!source || !previewDialog || !dialogImage || !dialogTitle) {
      return;
    }

    dialogImage.src = source;
    dialogImage.alt = title;
    dialogTitle.textContent = title;

    if (typeof previewDialog.showModal === "function") {
      previewDialog.showModal();
    } else {
      window.open(source, "_blank", "noopener");
    }
  });
});

if (dialogClose && previewDialog) {
  dialogClose.addEventListener("click", () => {
    previewDialog.close();
  });

  previewDialog.addEventListener("click", (event) => {
    if (event.target === previewDialog) {
      previewDialog.close();
    }
  });
}
