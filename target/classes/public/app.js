/**
 * BOOKHAVEN POS & INVENTORY DASHBOARD
 * Pure Vanilla JavaScript (ES6+ Fetch API, Zero external frameworks)
 * Phase 8 & Phase 9: Real BCrypt Authentication, Session Control & Minimalist Editorial Redesign
 */

// Application State
const state = {
    books: [],
    inventoryBooks: [],
    publishers: [],
    authors: [],
    ownerMetrics: null,
    inventoryFilter: 'all', // 'all' | 'active' | 'archived' | 'lowStock'
    inventorySearch: '',
    customers: [],
    employees: [],
    orders: [],
    holds: [],
    salesSummary: null,
    cart: new Map(), // key: bookId, value: { book, quantity }
    currentFilter: 'all',
    searchQuery: '',
    selectedBookForRestock: null,
    selectedBookForEdit: null,
    selectedBookForHold: null,
    activeHoldFulfillment: null,
    currentUser: null, // { username, email, role, displayName, customerId, employeeId }
    token: localStorage.getItem('bookstore_auth_token') || null,
    authMode: 'signin' // 'signin' | 'signup'
};

// API Endpoints
const API = {
    authLogin: '/api/auth/login',
    authRegister: '/api/auth/register',
    authLogout: '/api/auth/logout',
    authMe: '/api/auth/me',
    books: '/api/books',
    checkout: '/api/checkout',
    orders: '/api/orders',
    customers: '/api/customers',
    employees: '/api/employees',
    salesSummary: '/api/dashboard/sales-summary',
    restock: (id) => `/api/books/${id}/restock`,
    receipt: (orderId) => `/api/orders/${orderId}/receipt`,

    // In-Store Shelf Hold & Pickup Endpoints
    holds: '/api/holds',
    holdFulfill: (id) => `/api/holds/${id}/fulfill`,
    holdCancel: (id) => `/api/holds/${id}`,

    // Phase 10 & 11: Owner Catalog, Lifecycle & Audit Endpoints
    ownerBooks: '/api/owner/books',
    ownerBookById: (id) => `/api/owner/books/${id}`,
    ownerStockAdjust: (id) => `/api/owner/books/${id}/stock`,
    ownerPublishers: '/api/owner/publishers',
    ownerAuthors: '/api/owner/authors',
    ownerMetrics: '/api/owner/metrics',
    ownerOrders: '/api/owner/orders',
    ownerResendReceipt: (id) => `/api/owner/orders/${id}/resend-receipt`
};

// DOM Elements Cache
const DOM = {
    // Navigation Tabs
    navTabs: document.querySelectorAll('.nav-tab'),
    tabPanes: document.querySelectorAll('.tab-pane'),
    tabPos: document.getElementById('tabPos'),
    tabHolds: document.getElementById('tabHolds'),
    holdsCountBadge: document.getElementById('holdsCountBadge'),
    tabInventory: document.getElementById('tabInventory'),
    tabDashboard: document.getElementById('tabDashboard'),
    tabCustomers: document.getElementById('tabCustomers'),
    tabEmployees: document.getElementById('tabEmployees'),
    tabOrders: document.getElementById('tabOrders'),
    ordersTabLabel: document.getElementById('ordersTabLabel'),
    ordersSectionTitle: document.getElementById('ordersSectionTitle'),

    // In-Store Holds Queue
    holdsTableBody: document.getElementById('holdsTableBody'),
    refreshHoldsBtn: document.getElementById('refreshHoldsBtn'),

    // In-Store Hold Reservation Modal
    holdModalBackdrop: document.getElementById('holdModalBackdrop'),
    closeHoldModalBtn: document.getElementById('closeHoldModalBtn'),
    cancelHoldBtn: document.getElementById('cancelHoldBtn'),
    holdReservationForm: document.getElementById('holdReservationForm'),
    confirmHoldBtn: document.getElementById('confirmHoldBtn'),
    holdBookPreviewCard: document.getElementById('holdBookPreviewCard'),
    holdBookId: document.getElementById('holdBookId'),
    holdCustName: document.getElementById('holdCustName'),
    holdCustPhone: document.getElementById('holdCustPhone'),
    holdCustEmail: document.getElementById('holdCustEmail'),
    holdPickupWindow: document.getElementById('holdPickupWindow'),
    holdSuccessView: document.getElementById('holdSuccessView'),
    holdSlipDetails: document.getElementById('holdSlipDetails'),
    closeHoldSuccessBtn: document.getElementById('closeHoldSuccessBtn'),

    // Auth & Role Widget
    authWidget: document.getElementById('authWidget'),
    userRoleBadge: document.getElementById('userRoleBadge'),
    roleLabel: document.getElementById('roleLabel'),
    openLoginModalBtn: document.getElementById('openLoginModalBtn'),
    loginBtnText: document.getElementById('loginBtnText'),
    logoutBtn: document.getElementById('logoutBtn'),

    // Auth Modal
    loginModalBackdrop: document.getElementById('loginModalBackdrop'),
    closeLoginModalBtn: document.getElementById('closeLoginModalBtn'),
    authModalTitle: document.getElementById('authModalTitle'),
    authTabSignIn: document.getElementById('authTabSignIn'),
    authTabSignUp: document.getElementById('authTabSignUp'),
    demoCredsBar: document.getElementById('demoCredsBar'),
    quickLoginOwnerBtn: document.getElementById('quickLoginOwnerBtn'),
    quickLoginAdminBtn: document.getElementById('quickLoginAdminBtn'),
    quickLoginCustomerBtn: document.getElementById('quickLoginCustomerBtn'),
    manualLoginForm: document.getElementById('manualLoginForm'),
    loginUsername: document.getElementById('loginUsername'),
    loginPassword: document.getElementById('loginPassword'),
    loginErrorMsg: document.getElementById('loginErrorMsg'),
    submitLoginBtn: document.getElementById('submitLoginBtn'),

    // Search & Filters (POS)
    searchInput: document.getElementById('searchInput'),
    clearSearchBtn: document.getElementById('clearSearchBtn'),
    filterPills: document.querySelectorAll('.filter-pills .pill'),
    resultsCount: document.getElementById('resultsCount'),
    refreshCatalogBtn: document.getElementById('refreshCatalogBtn'),
    booksGrid: document.getElementById('booksGrid'),

    // Cashier Billing Register (Cart Drawer)
    openCartBtn: document.getElementById('openCartBtn'),
    closeCartBtn: document.getElementById('closeCartBtn'),
    cartBtnLabel: document.getElementById('cartBtnLabel'),
    cartDrawer: document.getElementById('cartDrawer'),
    cartBackdrop: document.getElementById('cartBackdrop'),
    cartCountBadge: document.getElementById('cartCountBadge'),
    cartItemsList: document.getElementById('cartItemsList'),
    cartTotalQty: document.getElementById('cartTotalQty'),
    cartTotalPrice: document.getElementById('cartTotalPrice'),
    registerHoldBanner: document.getElementById('registerHoldBanner'),
    registerHoldCode: document.getElementById('registerHoldCode'),
    clearRegisterHoldBtn: document.getElementById('clearRegisterHoldBtn'),
    customerSelect: document.getElementById('customerSelect'),
    newCustomerCheckbox: document.getElementById('newCustomerCheckbox'),
    newCustomerFields: document.getElementById('newCustomerFields'),
    custFirstName: document.getElementById('custFirstName'),
    custLastName: document.getElementById('custLastName'),
    custEmail: document.getElementById('custEmail'),
    custPhone: document.getElementById('custPhone'),
    sendEmailReceiptCheckbox: document.getElementById('sendEmailReceiptCheckbox'),
    cashierSelect: document.getElementById('cashierSelect'),
    cashTenderBox: document.getElementById('cashTenderBox'),
    cashTenderedInput: document.getElementById('cashTenderedInput'),
    cashChangeDue: document.getElementById('cashChangeDue'),
    tenderExactBtn: document.getElementById('tenderExactBtn'),
    cardDetailsBox: document.getElementById('cardDetailsBox'),
    upiDetailsBox: document.getElementById('upiDetailsBox'),
    executeCheckoutBtn: document.getElementById('executeCheckoutBtn'),

    // Owner Overview Metrics Bar (Phase 11)
    ownerMetricsBar: document.getElementById('ownerMetricsBar'),
    metricTotalCatalog: document.getElementById('metricTotalCatalog'),
    metricActiveCatalog: document.getElementById('metricActiveCatalog'),
    metricLowStock: document.getElementById('metricLowStock'),
    metricTodaySales: document.getElementById('metricTodaySales'),
    metricTodayOrders: document.getElementById('metricTodayOrders'),
    metricTotalOrders: document.getElementById('metricTotalOrders'),
    metricTotalSales: document.getElementById('metricTotalSales'),

    // Inventory & Restock (Phase 11)
    inventoryTableBody: document.getElementById('inventoryTableBody'),
    refreshInventoryBtn: document.getElementById('refreshInventoryBtn'),
    inventorySearchInput: document.getElementById('inventorySearchInput'),
    clearInventorySearchBtn: document.getElementById('clearInventorySearchBtn'),
    invFilterPills: document.querySelectorAll('[data-inv-filter]'),
    openAddBookModalBtn: document.getElementById('openAddBookModalBtn'),

    // Quick Restock Modal
    restockModalBackdrop: document.getElementById('restockModalBackdrop'),
    closeRestockModalBtn: document.getElementById('closeRestockModalBtn'),
    cancelRestockBtn: document.getElementById('cancelRestockBtn'),
    confirmRestockBtn: document.getElementById('confirmRestockBtn'),
    restockModalTitle: document.getElementById('restockModalTitle'),
    restockBookTitle: document.getElementById('restockBookTitle'),
    restockCurrentStock: document.getElementById('restockCurrentStock'),
    restockProjectedStock: document.getElementById('restockProjectedStock'),
    restockAddInput: document.getElementById('restockAddInput'),
    restockReasonInput: document.getElementById('restockReasonInput'),

    // Add Book Modal
    addBookModalBackdrop: document.getElementById('addBookModalBackdrop'),
    closeAddBookModalBtn: document.getElementById('closeAddBookModalBtn'),
    cancelAddBookBtn: document.getElementById('cancelAddBookBtn'),
    confirmAddBookBtn: document.getElementById('confirmAddBookBtn'),
    newBookTitle: document.getElementById('newBookTitle'),
    newBookPrice: document.getElementById('newBookPrice'),
    newBookStock: document.getElementById('newBookStock'),
    newBookGenre: document.getElementById('newBookGenre'),
    newBookPublisher: document.getElementById('newBookPublisher'),
    newBookAuthorsList: document.getElementById('newBookAuthorsList'),
    authorSearchFilter: document.getElementById('authorSearchFilter'),
    toggleInlinePublisherBtn: document.getElementById('toggleInlinePublisherBtn'),
    inlinePublisherForm: document.getElementById('inlinePublisherForm'),
    closeInlinePublisherBtn: document.getElementById('closeInlinePublisherBtn'),
    inlinePubName: document.getElementById('inlinePubName'),
    inlinePubPhone: document.getElementById('inlinePubPhone'),
    inlinePubCity: document.getElementById('inlinePubCity'),
    inlinePubStreet: document.getElementById('inlinePubStreet'),
    inlinePubPincode: document.getElementById('inlinePubPincode'),
    saveInlinePubBtn: document.getElementById('saveInlinePubBtn'),
    toggleInlineAuthorBtn: document.getElementById('toggleInlineAuthorBtn'),
    inlineAuthorForm: document.getElementById('inlineAuthorForm'),
    closeInlineAuthorBtn: document.getElementById('closeInlineAuthorBtn'),
    inlineAuthorFirstName: document.getElementById('inlineAuthorFirstName'),
    inlineAuthorLastName: document.getElementById('inlineAuthorLastName'),
    inlineAuthorEmail: document.getElementById('inlineAuthorEmail'),
    inlineAuthorBio: document.getElementById('inlineAuthorBio'),
    saveInlineAuthorBtn: document.getElementById('saveInlineAuthorBtn'),

    // Edit Book Modal
    editBookModalBackdrop: document.getElementById('editBookModalBackdrop'),
    closeEditBookModalBtn: document.getElementById('closeEditBookModalBtn'),
    cancelEditBookBtn: document.getElementById('cancelEditBookBtn'),
    confirmEditBookBtn: document.getElementById('confirmEditBookBtn'),
    editBookId: document.getElementById('editBookId'),
    editBookTitle: document.getElementById('editBookTitle'),
    editBookPrice: document.getElementById('editBookPrice'),
    editBookStock: document.getElementById('editBookStock'),
    editBookGenre: document.getElementById('editBookGenre'),
    editBookPublisher: document.getElementById('editBookPublisher'),
    editBookStatus: document.getElementById('editBookStatus'),
    editBookAuthorsList: document.getElementById('editBookAuthorsList'),

    // Orders & Receipts
    ordersTableBody: document.getElementById('ordersTableBody'),
    refreshOrdersBtn: document.getElementById('refreshOrdersBtn'),

    // Customers Table & CRM
    customersTableBody: document.getElementById('customersTableBody'),
    refreshCustomersBtn: document.getElementById('refreshCustomersBtn'),

    // Staff Table
    employeesTableBody: document.getElementById('employeesTableBody'),
    refreshEmployeesBtn: document.getElementById('refreshEmployeesBtn'),

    // KPI Dashboard
    refreshDashboardBtn: document.getElementById('refreshDashboardBtn'),
    kpiTotalRevenue: document.getElementById('kpiTotalRevenue'),
    kpiTodayRevenue: document.getElementById('kpiTodayRevenue'),
    kpiTodayOrdersCount: document.getElementById('kpiTodayOrdersCount'),
    kpiTotalOrders: document.getElementById('kpiTotalOrders'),
    kpiBooksSold: document.getElementById('kpiBooksSold'),
    kpiAvgOrderValue: document.getElementById('kpiAvgOrderValue'),

    // Order Success Modal
    orderSuccessBackdrop: document.getElementById('orderSuccessBackdrop'),
    closeSuccessModalBtn: document.getElementById('closeSuccessModalBtn'),
    closeSuccessModalBtnBottom: document.getElementById('closeSuccessModalBtnBottom'),
    successCardDetails: document.getElementById('successCardDetails'),
    viewPdfInvoiceBtn: document.getElementById('viewPdfInvoiceBtn'),

    // Toast Container
    toastContainer: document.getElementById('toastContainer')
};

// ============================================================================
// AUTHENTICATED FETCH WRAPPER
// ============================================================================
async function authFetch(url, options = {}) {
    options.headers = options.headers || {};
    if (state.token) {
        options.headers['Authorization'] = `Bearer ${state.token}`;
    }
    options.credentials = 'include'; // Send session cookie
    return fetch(url, options);
}

// ============================================================================
// INITIALIZATION
// ============================================================================
document.addEventListener('DOMContentLoaded', async () => {
    try { initTabNavigation(); } catch (e) { console.error('initTabNavigation error:', e); }
    try { initAuthControls(); } catch (e) { console.error('initAuthControls error:', e); }
    try { initCartDrawer(); } catch (e) { console.error('initCartDrawer error:', e); }
    try { initSearchAndFilters(); } catch (e) { console.error('initSearchAndFilters error:', e); }
    try { initCheckoutControls(); } catch (e) { console.error('initCheckoutControls error:', e); }
    try { initHoldModal(); } catch (e) { console.error('initHoldModal error:', e); }
    try { initRestockModal(); } catch (e) { console.error('initRestockModal error:', e); }
    try { initAddBookModal(); } catch (e) { console.error('initAddBookModal error:', e); }
    try { initEditBookModal(); } catch (e) { console.error('initEditBookModal error:', e); }
    try { initInventoryControls(); } catch (e) { console.error('initInventoryControls error:', e); }

    // Check existing authentication session
    try { await checkAuthSession(); } catch (e) { console.error('checkAuthSession error:', e); }

    // Load initial catalog & common data
    try { loadCatalog(); } catch (e) { console.error('loadCatalog error:', e); }
    try { loadCustomers(); } catch (e) { console.error('loadCustomers error:', e); }
    try { loadEmployees(); } catch (e) { console.error('loadEmployees error:', e); }
    try { loadOrders(); } catch (e) { console.error('loadOrders error:', e); }
    try { loadHolds(); } catch (e) { console.error('loadHolds error:', e); }
    if (canAccessOwnerOrAdmin()) {
        try { loadDashboard(); } catch (e) {}
        try { loadInventory(); } catch (e) {}
        try { loadOwnerMetrics(); } catch (e) {}
    }
});

// ============================================================================
// TAB NAVIGATION & ROLE VISIBILITY
// ============================================================================
function initTabNavigation() {
    DOM.navTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const targetPaneId = tab.getAttribute('data-tab');
            switchTab(targetPaneId);
        });
    });

    if (DOM.refreshHoldsBtn) {
        DOM.refreshHoldsBtn.addEventListener('click', () => loadHolds());
    }
    if (DOM.refreshOrdersBtn) {
        DOM.refreshOrdersBtn.addEventListener('click', () => loadOrders());
    }
    if (DOM.refreshCustomersBtn) {
        DOM.refreshCustomersBtn.addEventListener('click', () => loadCustomers());
    }
    if (DOM.refreshEmployeesBtn) {
        DOM.refreshEmployeesBtn.addEventListener('click', () => loadEmployees());
    }
    if (DOM.refreshDashboardBtn) {
        DOM.refreshDashboardBtn.addEventListener('click', () => loadDashboard());
    }
    if (DOM.refreshInventoryBtn) {
        DOM.refreshInventoryBtn.addEventListener('click', () => {
            loadInventory();
            loadOwnerMetrics();
        });
    }
}

function switchTab(tabName) {
    DOM.navTabs.forEach(t => {
        if (t.getAttribute('data-tab') === tabName) {
            t.classList.add('active');
        } else {
            t.classList.remove('active');
        }
    });

    DOM.tabPanes.forEach(pane => {
        const paneName = pane.id.replace('pane', '').toLowerCase();
        if (paneName === tabName.toLowerCase()) {
            pane.classList.add('active');
        } else {
            pane.classList.remove('active');
        }
    });

    // Refresh context-specific data
    if (tabName === 'holds') loadHolds();
    if (tabName === 'dashboard') loadDashboard();
    if (tabName === 'customers') loadCustomers();
    if (tabName === 'employees') loadEmployees();
    if (tabName === 'orders') loadOrders();
    if (tabName === 'inventory') {
        loadInventory();
        loadOwnerMetrics();
    }
}

function canAccessOwnerOrAdmin() {
    const role = state.currentUser ? state.currentUser.role : null;
    return role === 'BOOKSTORE_OWNER' || role === 'ADMIN';
}

function applyRoleVisibility() {
    const role = state.currentUser ? state.currentUser.role : null;
    const name = state.currentUser ? (state.currentUser.displayName || state.currentUser.email || state.currentUser.username) : 'Guest';

    // 1. Update Header Role Badge
    DOM.userRoleBadge.className = 'role-pill';
    if (role === 'BOOKSTORE_OWNER') {
        DOM.userRoleBadge.classList.add('role-owner');
        DOM.roleLabel.textContent = `[ROLE: OWNER]`;
        DOM.loginBtnText.textContent = 'Switch';
        DOM.logoutBtn.classList.remove('hidden');
    } else if (role === 'ADMIN') {
        DOM.userRoleBadge.classList.add('role-admin');
        DOM.roleLabel.textContent = `[ROLE: ADMIN]`;
        DOM.loginBtnText.textContent = 'Switch';
        DOM.logoutBtn.classList.remove('hidden');
    } else if (role === 'CUSTOMER') {
        DOM.userRoleBadge.classList.add('role-customer');
        DOM.roleLabel.textContent = `[CUSTOMER: ${name.split('@')[0]}]`;
        DOM.loginBtnText.textContent = 'Switch';
        DOM.logoutBtn.classList.remove('hidden');
    } else {
        DOM.roleLabel.textContent = '[GUEST]';
        DOM.loginBtnText.textContent = 'Sign In';
        DOM.logoutBtn.classList.add('hidden');
    }

    // 2. Tab Visibility Rules
    // - POS (Catalog): Always visible
    // - Inventory: OWNER, ADMIN
    // - Analytics (Dashboard): OWNER, ADMIN
    // - Customers CRM: OWNER, ADMIN
    // - Staff (EMPLOYEE): ADMIN only
    // - Orders: CUSTOMER, OWNER, ADMIN
    DOM.tabPos.style.display = 'inline-flex';

    if (role === 'ADMIN') {
        if (DOM.tabHolds) DOM.tabHolds.style.display = 'inline-flex';
        DOM.tabInventory.style.display = 'inline-flex';
        DOM.tabDashboard.style.display = 'inline-flex';
        DOM.tabCustomers.style.display = 'inline-flex';
        DOM.tabEmployees.style.display = 'inline-flex';
        DOM.tabOrders.style.display = 'inline-flex';
        DOM.ordersTabLabel.textContent = 'Orders';
        if (DOM.ordersSectionTitle) DOM.ordersSectionTitle.textContent = 'Master Order History & Digital Receipts';
    } else if (role === 'BOOKSTORE_OWNER') {
        if (DOM.tabHolds) DOM.tabHolds.style.display = 'inline-flex';
        DOM.tabInventory.style.display = 'inline-flex';
        DOM.tabDashboard.style.display = 'inline-flex';
        DOM.tabCustomers.style.display = 'inline-flex';
        DOM.tabEmployees.style.display = 'none'; // Staff management is Admin only
        DOM.tabOrders.style.display = 'inline-flex';
        DOM.ordersTabLabel.textContent = 'Orders';
        if (DOM.ordersSectionTitle) DOM.ordersSectionTitle.textContent = 'Store Order History & Digital Receipts';
    } else if (role === 'CUSTOMER') {
        if (DOM.tabHolds) DOM.tabHolds.style.display = 'inline-flex';
        DOM.tabInventory.style.display = 'none';
        DOM.tabDashboard.style.display = 'none';
        DOM.tabCustomers.style.display = 'none';
        DOM.tabEmployees.style.display = 'none';
        DOM.tabOrders.style.display = 'inline-flex';
        DOM.ordersTabLabel.textContent = 'My Orders';
        if (DOM.ordersSectionTitle) DOM.ordersSectionTitle.textContent = 'My Order History & Invoices';
    } else {
        // Guest unauthenticated
        if (DOM.tabHolds) DOM.tabHolds.style.display = 'none';
        DOM.tabInventory.style.display = 'none';
        DOM.tabDashboard.style.display = 'none';
        DOM.tabCustomers.style.display = 'none';
        DOM.tabEmployees.style.display = 'none';
        DOM.tabOrders.style.display = 'none';
    }

    const isStaff = (role === 'BOOKSTORE_OWNER' || role === 'ADMIN');
    if (DOM.openCartBtn) {
        DOM.openCartBtn.style.display = 'inline-flex';
        if (DOM.cartBtnLabel) {
            DOM.cartBtnLabel.textContent = isStaff ? 'Cashier Register' : 'Register';
        }
    }

    // If active tab is now hidden, fallback to Catalog
    const activeTab = document.querySelector('.nav-tab.active');
    if (activeTab && activeTab.style.display === 'none') {
        switchTab('pos');
    }

    // Pre-fill customer email in checkout if customer is logged in
    if (role === 'CUSTOMER' && state.currentUser) {
        const userEmail = state.currentUser.email || state.currentUser.username || '';
        if (DOM.custEmail) DOM.custEmail.value = userEmail;
        if (DOM.custFirstName) DOM.custFirstName.value = state.currentUser.displayName || userEmail.split('@')[0];
    }
}

// ============================================================================
// AUTHENTICATION & LOGIN CONTROLS
// ============================================================================
function initAuthControls() {
    if (DOM.openLoginModalBtn) DOM.openLoginModalBtn.addEventListener('click', openLoginModal);
    if (DOM.closeLoginModalBtn) DOM.closeLoginModalBtn.addEventListener('click', closeLoginModal);
    if (DOM.loginModalBackdrop) {
        DOM.loginModalBackdrop.addEventListener('click', (e) => {
            if (e.target === DOM.loginModalBackdrop) closeLoginModal();
        });
    }

    // Toggle between Sign In and Sign Up tabs
    if (DOM.authTabSignIn) DOM.authTabSignIn.addEventListener('click', () => setAuthMode('signin'));
    if (DOM.authTabSignUp) DOM.authTabSignUp.addEventListener('click', () => setAuthMode('signup'));

    // Quick 1-Click Role Logins (using BCrypt seeded credentials)
    if (DOM.quickLoginOwnerBtn) {
        DOM.quickLoginOwnerBtn.addEventListener('click', () => executeLogin('owner@bookstore.com', 'Owner@123'));
    }
    if (DOM.quickLoginAdminBtn) {
        DOM.quickLoginAdminBtn.addEventListener('click', () => executeLogin('admin@bookstore.com', 'Admin@123'));
    }
    if (DOM.quickLoginCustomerBtn) {
        DOM.quickLoginCustomerBtn.addEventListener('click', () => executeLogin('reader@gmail.com', 'Reader@123'));
    }

    // Manual Form Submit
    if (DOM.manualLoginForm) {
        DOM.manualLoginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const email = DOM.loginUsername.value.trim();
            const password = DOM.loginPassword.value;
            if (!email || !password) return;

            if (state.authMode === 'signup') {
                await executeRegister(email, password);
            } else {
                await executeLogin(email, password);
            }
        });
    }

    // Logout
    if (DOM.logoutBtn) DOM.logoutBtn.addEventListener('click', executeLogout);
}

function setAuthMode(mode) {
    state.authMode = mode;
    DOM.loginErrorMsg.classList.add('hidden');

    if (mode === 'signup') {
        DOM.authTabSignUp.classList.add('active');
        DOM.authTabSignIn.classList.remove('active');
        DOM.authModalTitle.textContent = 'Create Customer Account';
        DOM.submitLoginBtn.textContent = 'Create Account';
        DOM.demoCredsBar.classList.add('hidden');
    } else {
        DOM.authTabSignIn.classList.add('active');
        DOM.authTabSignUp.classList.remove('active');
        DOM.authModalTitle.textContent = 'Account Access';
        DOM.submitLoginBtn.textContent = 'Sign In';
        DOM.demoCredsBar.classList.remove('hidden');
    }
}

function openLoginModal() {
    setAuthMode('signin');
    DOM.loginErrorMsg.classList.add('hidden');
    DOM.loginUsername.value = '';
    DOM.loginPassword.value = '';
    DOM.loginModalBackdrop.classList.add('open');
}

function closeLoginModal() {
    DOM.loginModalBackdrop.classList.remove('open');
}

async function checkAuthSession() {
    try {
        const res = await authFetch(API.authMe);
        if (res.ok) {
            const data = await res.json();
            if (data.authenticated && data.user) {
                state.currentUser = {
                    id: data.user.id,
                    email: data.user.email,
                    username: data.user.email,
                    role: data.user.role,
                    displayName: data.user.email.split('@')[0]
                };
            } else if (data.email) {
                state.currentUser = {
                    email: data.email,
                    username: data.email,
                    role: data.role,
                    displayName: data.displayName || data.email.split('@')[0]
                };
            }
            applyRoleVisibility();
            console.log(`[BookHaven] Active session: ${state.currentUser.email} (${state.currentUser.role})`);
        } else {
            state.currentUser = null;
            applyRoleVisibility();
        }
    } catch (e) {
        state.currentUser = null;
        applyRoleVisibility();
    }
}

async function executeLogin(username, password) {
    try {
        DOM.submitLoginBtn.disabled = true;
        DOM.loginErrorMsg.classList.add('hidden');

        const res = await fetch(API.authLogin, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ username, password })
        });

        const data = await res.json();
        if (!res.ok) {
            throw new Error(data.message || data.error || 'Authentication failed');
        }

        // Store token
        state.token = data.token;
        if (data.token) {
            localStorage.setItem('bookstore_auth_token', data.token);
        }

        state.currentUser = {
            email: data.email || data.username,
            username: data.username || data.email,
            role: data.role,
            displayName: data.displayName || (data.email || data.username).split('@')[0],
            customerId: data.customerId,
            employeeId: data.employeeId
        };

        applyRoleVisibility();
        closeLoginModal();
        showToast(`Signed in as ${state.currentUser.email} (${data.role})`, 'success');

        // Reload fresh data under this role's permissions
        loadCatalog();
        loadOrders();
        if (canAccessOwnerOrAdmin()) {
            loadDashboard();
            loadCustomers();
        }
        if (data.role === 'ADMIN') {
            loadEmployees();
        }

    } catch (err) {
        DOM.loginErrorMsg.textContent = err.message;
        DOM.loginErrorMsg.classList.remove('hidden');
        showToast(err.message, 'error');
    } finally {
        DOM.submitLoginBtn.disabled = false;
    }
}

async function executeRegister(email, password) {
    try {
        DOM.submitLoginBtn.disabled = true;
        DOM.loginErrorMsg.classList.add('hidden');

        const res = await fetch(API.authRegister, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ email, password })
        });

        const data = await res.json();
        if (!res.ok) {
            throw new Error(data.message || data.error || 'Registration failed');
        }

        // Store token
        state.token = data.token;
        if (data.token) {
            localStorage.setItem('bookstore_auth_token', data.token);
        }

        state.currentUser = {
            id: data.id,
            email: data.email,
            username: data.email,
            role: data.role,
            displayName: data.email.split('@')[0]
        };

        applyRoleVisibility();
        closeLoginModal();
        showToast(`Account registered! Logged in as ${data.email} (${data.role})`, 'success');

        // Reload data
        loadCatalog();
        loadOrders();
        if (canAccessOwnerOrAdmin()) {
            loadDashboard();
            loadCustomers();
        }
        if (data.role === 'ADMIN') {
            loadEmployees();
        }

    } catch (err) {
        DOM.loginErrorMsg.textContent = err.message;
        DOM.loginErrorMsg.classList.remove('hidden');
        showToast(err.message, 'error');
    } finally {
        DOM.submitLoginBtn.disabled = false;
    }
}

async function executeLogout() {
    try {
        await authFetch(API.authLogout, { method: 'POST' });
    } catch (e) {}

    state.token = null;
    state.currentUser = null;
    localStorage.removeItem('bookstore_auth_token');

    applyRoleVisibility();
    showToast('Signed out successfully', 'info');

    // Reload catalog under guest view
    loadCatalog();
    loadOrders();
}

// ============================================================================
// CATALOG DATA & RENDERING (EDITORIAL COMPACT BOOKSHELF)
// ============================================================================
async function loadCatalog(query = '', filter = 'all') {
    try {
        DOM.resultsCount.textContent = 'Loading catalog from MySQL...';
        const res = await fetch(API.books);
        if (!res.ok) throw new Error('Failed to fetch book catalog');
        const books = await res.json();
        state.books = books;

        filterAndRenderCatalog(query, filter);
        renderInventoryTable();
    } catch (err) {
        DOM.resultsCount.textContent = 'Error connecting to database.';
        showToast(err.message, 'error');
    }
}

function filterAndRenderCatalog(query = '', filter = 'all') {
    let filtered = [...state.books];

    // Filter by category or low stock
    if (filter === 'lowStock') {
        filtered = filtered.filter(b => b.stockCount <= 5);
    } else if (filter && filter !== 'all') {
        filtered = filtered.filter(b => b.genre && b.genre.toLowerCase() === filter.toLowerCase());
    }

    // Search query filter
    if (query) {
        const q = query.toLowerCase();
        filtered = filtered.filter(b => {
            const titleMatch = b.title && b.title.toLowerCase().includes(q);
            const genreMatch = b.genre && b.genre.toLowerCase().includes(q);
            const pubMatch = b.publisherName && b.publisherName.toLowerCase().includes(q);
            const authorMatch = b.authors && b.authors.some(a => a.toLowerCase().includes(q));
            return titleMatch || genreMatch || pubMatch || authorMatch;
        });
    }

    renderBooksGrid(filtered);
    DOM.resultsCount.textContent = `Showing ${filtered.length} of ${state.books.length} books`;
}

function renderBooksGrid(books) {
    if (!books || books.length === 0) {
        DOM.booksGrid.innerHTML = `
            <div class="empty-state">
                <h3>No books match your criteria</h3>
                <p>Try clearing filters or checking inventory levels.</p>
            </div>
        `;
        return;
    }

    DOM.booksGrid.innerHTML = books.map(book => {
        const isOutOfStock = book.stockCount <= 0;
        const isLowStock = book.stockCount <= 5 && book.stockCount > 0;
        const authors = (book.authors && book.authors.length > 0) ? book.authors.join(', ') : 'Unknown Author';

        let stockClass = 'in-stock';
        let stockLabel = `In Shop (${book.stockCount})`;
        if (isOutOfStock) {
            stockClass = 'out-of-stock';
            stockLabel = 'Out of Stock on Shelves';
        } else if (isLowStock) {
            stockClass = 'low-stock';
            stockLabel = `Low Shelf Stock (${book.stockCount} left)`;
        }

        const isStaff = canAccessOwnerOrAdmin();
        const shelfLocation = book.shelfLocation || 'Aisle 1, Main Bay';

        return `
            <article class="book-card ${isOutOfStock ? 'card-disabled' : ''}" data-id="${book.bookId}">
                <div>
                    <div class="book-meta-top">
                        <span class="genre-tag">${escapeHtml(book.genre || 'General')}</span>
                        <span class="stock-tag ${stockClass}">${stockLabel}</span>
                    </div>

                    <div class="shelf-location-badge" title="Physical bookstore shelf & bay location">
                        📍 ${escapeHtml(shelfLocation)}
                    </div>

                    <h3 class="book-title" title="${escapeHtml(book.title)}">${escapeHtml(book.title)}</h3>
                    <p class="book-author">by ${escapeHtml(authors)}</p>
                    <p class="book-publisher">${escapeHtml(book.publisherName || 'Independent')}</p>
                </div>

                <div class="book-footer">
                    <div class="price-display">
                        <span class="price-label">Counter Price</span>
                        <span class="price-val">₹${Number(book.price).toFixed(2)}</span>
                    </div>

                    <div class="card-actions">
                        ${isStaff ? `
                            <button class="btn btn-secondary btn-sm" onclick="openQuickRestockModal(${book.bookId})" title="Restock inventory">
                                Restock
                            </button>
                            <button class="btn btn-primary btn-sm add-cart-btn" 
                                    onclick="addToCart(${book.bookId})" 
                                    ${isOutOfStock ? 'disabled' : ''} title="Ring up at cashier billing desk">
                                + Add to Register
                            </button>
                        ` : `
                            <button class="btn btn-primary btn-sm" style="width: 100%; justify-content: center;"
                                    onclick="openHoldModal(${book.bookId})" 
                                    ${isOutOfStock ? 'disabled' : ''} title="Hold copy behind the counter for 48 hours">
                                📍 Reserve for In-Store Pickup
                            </button>
                        `}
                    </div>
                </div>
            </article>
        `;
    }).join('');
}

// ============================================================================
// SEARCH & FILTERS
// ============================================================================
function initSearchAndFilters() {
    DOM.searchInput.addEventListener('input', (e) => {
        state.searchQuery = e.target.value.trim();
        DOM.clearSearchBtn.style.display = state.searchQuery ? 'block' : 'none';
        filterAndRenderCatalog(state.searchQuery, state.currentFilter);
    });

    DOM.clearSearchBtn.addEventListener('click', () => {
        DOM.searchInput.value = '';
        state.searchQuery = '';
        DOM.clearSearchBtn.style.display = 'none';
        filterAndRenderCatalog('', state.currentFilter);
    });

    DOM.filterPills.forEach(pill => {
        pill.addEventListener('click', () => {
            DOM.filterPills.forEach(p => p.classList.remove('active'));
            pill.classList.add('active');
            state.currentFilter = pill.getAttribute('data-filter');
            filterAndRenderCatalog(state.searchQuery, state.currentFilter);
        });
    });

    DOM.refreshCatalogBtn.addEventListener('click', () => {
        loadCatalog(state.searchQuery, state.currentFilter);
        showToast('Catalog refreshed from MySQL.', 'info');
    });
}

// ============================================================================
// SHOPPING CART & CHECKOUT
// ============================================================================
function initCartDrawer() {
    if (DOM.openCartBtn) DOM.openCartBtn.addEventListener('click', openCart);
    if (DOM.closeCartBtn) DOM.closeCartBtn.addEventListener('click', closeCart);
    if (DOM.cartBackdrop) DOM.cartBackdrop.addEventListener('click', closeCart);
}

function openCart() {
    DOM.cartDrawer.classList.add('open');
    DOM.cartBackdrop.classList.add('open');
    renderCart();
}

function closeCart() {
    DOM.cartDrawer.classList.remove('open');
    DOM.cartBackdrop.classList.remove('open');
}

window.addToCart = function(bookId) {
    const book = state.books.find(b => b.bookId === bookId);
    if (!book || book.stockCount <= 0) {
        showToast('This book is currently out of stock.', 'error');
        return;
    }

    const currentQty = state.cart.has(bookId) ? state.cart.get(bookId).quantity : 0;
    if (currentQty + 1 > book.stockCount) {
        showToast(`Cannot add more. Only ${book.stockCount} units available in stock.`, 'error');
        return;
    }

    state.cart.set(bookId, { book, quantity: currentQty + 1 });
    updateCartCountBadge();
    showToast(`Added "${book.title}" to cart`, 'success');
};

function updateCartCountBadge() {
    let totalItems = 0;
    for (const item of state.cart.values()) {
        totalItems += item.quantity;
    }
    DOM.cartCountBadge.textContent = totalItems;
    DOM.cartTotalQty.textContent = totalItems;
}

function renderCart() {
    updateCartCountBadge();

    if (state.cart.size === 0) {
        DOM.cartItemsList.innerHTML = `
            <div class="empty-cart-state">
                <p>Register bill is empty.</p>
                <span class="subtext">Click "+ Add to Register" on books brought to the billing desk.</span>
            </div>
        `;
        DOM.cartTotalPrice.textContent = '₹0.00';
        DOM.executeCheckoutBtn.disabled = true;
        updateCashTenderCalculations(0);
        return;
    }

    let totalPrice = 0;
    let itemsHtml = '';

    for (const [bookId, item] of state.cart.entries()) {
        const lineTotal = item.book.price * item.quantity;
        totalPrice += lineTotal;

        itemsHtml += `
            <div class="cart-item-row" data-id="${bookId}">
                <div class="cart-item-details">
                    <span class="cart-item-title">${escapeHtml(item.book.title)}</span>
                    <span class="cart-item-meta">₹${Number(item.book.price).toFixed(2)} each &bull; In Shop: ${item.book.stockCount}</span>
                </div>
                <div class="cart-item-qty-controls">
                    <button class="qty-btn" onclick="updateCartItemQty(${bookId}, -1)">&minus;</button>
                    <span class="qty-val">${item.quantity}</span>
                    <button class="qty-btn" onclick="updateCartItemQty(${bookId}, 1)" ${item.quantity >= item.book.stockCount ? 'disabled' : ''}>&plus;</button>
                </div>
                <div class="num-cell" style="font-size: 0.85rem; width: 64px; text-align: right; font-weight: 700;">₹${lineTotal.toFixed(2)}</div>
                <button class="cart-remove-btn" onclick="removeFromCart(${bookId})" title="Remove item">&times;</button>
            </div>
        `;
    }

    DOM.cartItemsList.innerHTML = itemsHtml;
    DOM.cartTotalPrice.textContent = `₹${totalPrice.toFixed(2)}`;
    DOM.executeCheckoutBtn.disabled = false;
    updateCashTenderCalculations(totalPrice);
}

window.updateCartItemQty = function(bookId, delta) {
    if (!state.cart.has(bookId)) return;
    const item = state.cart.get(bookId);
    const newQty = item.quantity + delta;

    if (newQty <= 0) {
        state.cart.delete(bookId);
    } else if (newQty > item.book.stockCount) {
        showToast(`Stock limit reached (${item.book.stockCount} units available).`, 'error');
        return;
    } else {
        item.quantity = newQty;
    }
    renderCart();
};

window.removeFromCart = function(bookId) {
    state.cart.delete(bookId);
    renderCart();
};

// ============================================================================
// CHECKOUT CONTROLS & CASH DRAWER TENDER DISPATCH
// ============================================================================
function getCartTotal() {
    let total = 0;
    for (const item of state.cart.values()) {
        total += item.book.price * item.quantity;
    }
    return total;
}

function updateCashTenderCalculations(total = null) {
    if (total === null) total = getCartTotal();
    const tendered = parseFloat(DOM.cashTenderedInput ? DOM.cashTenderedInput.value : 0) || 0;
    const change = Math.max(0, tendered - total);
    if (DOM.cashChangeDue) {
        DOM.cashChangeDue.textContent = `₹${change.toFixed(2)}`;
        DOM.cashChangeDue.style.color = (tendered >= total && total > 0) ? 'var(--action-success)' : 'var(--text-muted)';
    }
}

function initCashTenderControls() {
    if (DOM.cashTenderedInput) {
        DOM.cashTenderedInput.addEventListener('input', () => updateCashTenderCalculations());
    }

    if (DOM.tenderExactBtn) {
        DOM.tenderExactBtn.addEventListener('click', () => {
            const total = getCartTotal();
            if (DOM.cashTenderedInput) {
                DOM.cashTenderedInput.value = total > 0 ? total.toFixed(2) : '0.00';
                updateCashTenderCalculations(total);
            }
        });
    }

    document.querySelectorAll('.quick-tender-chips [data-tender]').forEach(btn => {
        btn.addEventListener('click', () => {
            const add = parseFloat(btn.getAttribute('data-tender')) || 0;
            const current = parseFloat(DOM.cashTenderedInput.value) || 0;
            DOM.cashTenderedInput.value = (current + add).toFixed(2);
            updateCashTenderCalculations();
        });
    });

    if (DOM.clearRegisterHoldBtn) {
        DOM.clearRegisterHoldBtn.addEventListener('click', () => {
            state.activeHoldFulfillment = null;
            if (DOM.registerHoldBanner) DOM.registerHoldBanner.classList.add('hidden');
            showToast('Cleared in-store hold from register.', 'info');
        });
    }
}

function initCheckoutControls() {
    initCashTenderControls();

    DOM.newCustomerCheckbox.addEventListener('change', (e) => {
        if (e.target.checked) {
            DOM.newCustomerFields.classList.remove('hidden');
            DOM.customerSelect.disabled = true;
        } else {
            DOM.newCustomerFields.classList.add('hidden');
            DOM.customerSelect.disabled = false;
        }
    });

    // Payment method pill selection & box switching
    document.querySelectorAll('input[name="payMethod"]').forEach(radio => {
        radio.addEventListener('change', () => {
            document.querySelectorAll('.payment-option').forEach(el => el.classList.remove('active'));
            radio.closest('.payment-option').classList.add('active');

            const val = radio.value;
            if (DOM.cashTenderBox) DOM.cashTenderBox.classList.toggle('hidden', val !== 'CASH');
            if (DOM.cardDetailsBox) DOM.cardDetailsBox.classList.toggle('hidden', val !== 'CREDIT_CARD');
            if (DOM.upiDetailsBox) DOM.upiDetailsBox.classList.toggle('hidden', val !== 'UPI');
        });
    });

    DOM.executeCheckoutBtn.addEventListener('click', executeCheckout);
}

async function loadCustomers() {
    try {
        const res = await authFetch(API.customers);
        if (!res.ok) {
            DOM.customerSelect.innerHTML = `<option value="">Guest Walk-in Shopper</option>`;
            return;
        }
        const customers = await res.json();
        state.customers = customers;

        DOM.customerSelect.innerHTML = customers.map(c => `
            <option value="${c.customerId}">${escapeHtml(c.firstName + ' ' + c.lastName)} (${escapeHtml(c.emailId)})</option>
        `).join('');

        renderCustomersTable();
    } catch (e) {
        DOM.customerSelect.innerHTML = `<option value="">Guest Shopper</option>`;
    }
}

async function loadEmployees() {
    try {
        const res = await authFetch(API.employees);
        if (!res.ok) {
            DOM.cashierSelect.innerHTML = `<option value="1">Cashier Station #1</option>`;
            return;
        }
        const employees = await res.json();
        state.employees = employees;

        DOM.cashierSelect.innerHTML = employees.map(e => `
            <option value="${e.employeeId}">${escapeHtml(e.firstName + ' ' + e.lastName)} (${escapeHtml(e.role || 'Staff')})</option>
        `).join('');

        renderEmployeesTable();
    } catch (e) {
        DOM.cashierSelect.innerHTML = `<option value="1">Cashier Station #1</option>`;
    }
}

function renderCustomersTable() {
    if (!DOM.customersTableBody) return;
    if (!state.customers || state.customers.length === 0) {
        DOM.customersTableBody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 2rem;">No registered customers found.</td></tr>`;
        return;
    }
    DOM.customersTableBody.innerHTML = state.customers.map(c => `
        <tr>
            <td class="num-cell">#${c.customerId}</td>
            <td><strong>${escapeHtml(c.firstName + ' ' + c.lastName)}</strong></td>
            <td style="color: var(--text-muted); font-size: 0.85rem;"><a href="mailto:${escapeHtml(c.emailId || '')}" style="color: var(--text-main);">${escapeHtml(c.emailId || 'N/A')}</a></td>
            <td>${escapeHtml(c.street || '—')}</td>
            <td>${escapeHtml(c.city || '—')}</td>
            <td class="num-cell">${escapeHtml(c.pincode || '—')}</td>
        </tr>
    `).join('');
}

function renderEmployeesTable() {
    if (!DOM.employeesTableBody) return;
    if (!state.employees || state.employees.length === 0) {
        DOM.employeesTableBody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--text-muted); padding: 2rem;">No staff records available.</td></tr>`;
        return;
    }
    DOM.employeesTableBody.innerHTML = state.employees.map(e => `
        <tr>
            <td class="num-cell">#${e.employeeId}</td>
            <td><strong>${escapeHtml(e.firstName + ' ' + e.lastName)}</strong></td>
            <td><span class="role-pill ${e.role ? e.role.toLowerCase() : ''}">${escapeHtml(e.role || 'Staff')}</span></td>
            <td class="num-cell" style="font-weight: 600;">₹${Number(e.salary || 0).toFixed(2)}</td>
            <td><span class="status-pill active">Active</span></td>
        </tr>
    `).join('');
}

async function executeCheckout() {
    if (state.cart.size === 0) return;

    try {
        DOM.executeCheckoutBtn.disabled = true;
        DOM.executeCheckoutBtn.textContent = 'Processing Counter Bill...';

        // 1. Build Payload
        const payload = {
            items: [],
            paymentMethod: document.querySelector('input[name="payMethod"]:checked').value,
            employeeId: DOM.cashierSelect.value ? parseInt(DOM.cashierSelect.value, 10) : null,
            sendEmailReceipt: DOM.sendEmailReceiptCheckbox.checked
        };

        if (payload.paymentMethod === 'CASH' && DOM.cashTenderedInput && DOM.cashTenderedInput.value) {
            const tendered = parseFloat(DOM.cashTenderedInput.value);
            if (!isNaN(tendered)) {
                payload.cashTendered = tendered;
            }
        }

        if (state.activeHoldFulfillment) {
            payload.holdId = state.activeHoldFulfillment.holdId;
        }

        for (const [bookId, item] of state.cart.entries()) {
            payload.items.push({ bookId, quantity: item.quantity });
        }

        // Customer selection or new customer
        if (DOM.newCustomerCheckbox.checked) {
            payload.customerEmail = DOM.custEmail.value.trim();
            payload.customerFirstName = DOM.custFirstName.value.trim();
            payload.customerLastName = DOM.custLastName.value.trim();
            payload.customerPhone = DOM.custPhone.value.trim();

            if (!payload.customerEmail && DOM.sendEmailReceiptCheckbox.checked) {
                throw new Error('Customer Email is required for digital receipt dispatch.');
            }
        } else {
            const selectedCustId = DOM.customerSelect.value;
            if (selectedCustId) {
                payload.customerId = parseInt(selectedCustId, 10);
            }
        }

        // 2. Atomic POST Request
        const res = await authFetch(API.checkout, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();

        if (!res.ok) {
            throw new Error(data.message || data.error || 'Counter checkout failed');
        }

        // 3. Checkout succeeded!
        if (state.activeHoldFulfillment) {
            const holdCode = state.activeHoldFulfillment.holdCode;
            state.activeHoldFulfillment = null;
            if (DOM.registerHoldBanner) DOM.registerHoldBanner.classList.add('hidden');
            showToast(`In-store hold ${holdCode} fulfilled at counter!`, 'success');
        }

        state.cart.clear();
        updateCartCountBadge();
        closeCart();
        showOrderSuccessModal(data);

        // Reset cash tendered
        if (DOM.cashTenderedInput) DOM.cashTenderedInput.value = '';
        updateCashTenderCalculations(0);

        // Refresh live data
        loadCatalog(state.searchQuery, state.currentFilter);
        loadOrders();
        loadHolds();
        if (canAccessOwnerOrAdmin()) {
            loadDashboard();
        }

    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        DOM.executeCheckoutBtn.disabled = false;
        DOM.executeCheckoutBtn.textContent = 'Settle Counter Bill & Print Receipt';
    }
}

function showOrderSuccessModal(res) {
    DOM.orderSuccessBackdrop.classList.add('open');

    let emailBadgeHtml = '';
    if (res.emailReceiptDispatched) {
        emailBadgeHtml = `
            <div style="background: var(--action-success-bg); border: 1px solid #bbf7d0; padding: 0.65rem 0.85rem; border-radius: var(--radius-sm); margin-top: 0.75rem; font-size: 0.8rem; color: var(--action-success); font-weight: 600;">
                ✉️ Digital OpenPDF receipt dispatched to <strong>${escapeHtml(res.customerEmail)}</strong> via Jakarta Mail!
            </div>
        `;
    }

    let cashierHtml = '';
    if (res.employeeId) {
        cashierHtml = `<div class="receipt-row"><span>Attending Cashier:</span> <span>Staff #${res.employeeId}</span></div>`;
    }

    let cashDetailsHtml = '';
    if (res.paymentMethod === 'CASH' && res.cashTendered != null) {
        cashDetailsHtml = `
            <div class="receipt-row"><span>Cash Tendered:</span> <span class="num-cell">₹${Number(res.cashTendered).toFixed(2)}</span></div>
            <div class="receipt-row bold" style="color: var(--action-success);"><span>Change Returned:</span> <span class="num-cell">₹${Number(res.changeDue || 0).toFixed(2)}</span></div>
        `;
    }

    DOM.successCardDetails.innerHTML = `
        <div class="receipt-row"><span>Order ID:</span> <span class="num-cell">#${res.orderId}</span></div>
        <div class="receipt-row"><span>Payment ID:</span> <span class="num-cell">#${res.paymentId} (${res.paymentMethod})</span></div>
        ${cashierHtml}
        <div class="receipt-row"><span>Customer:</span> <strong>${escapeHtml(res.customerName)}</strong></div>
        <div class="receipt-row"><span>Customer Email:</span> <span>${escapeHtml(res.customerEmail || 'N/A')}</span></div>
        <div class="receipt-row"><span>Items Count:</span> <span class="num-cell">${res.totalQuantity} unit(s)</span></div>
        <div class="receipt-row bold"><span>Total Settle Amount:</span> <span class="num-cell" style="color: var(--action-success);">₹${Number(res.totalAmount).toFixed(2)}</span></div>
        ${cashDetailsHtml}
        ${emailBadgeHtml}
    `;

    DOM.viewPdfInvoiceBtn.href = API.receipt(res.orderId);
}

DOM.closeSuccessModalBtn.addEventListener('click', () => {
    DOM.orderSuccessBackdrop.classList.remove('open');
});
if (DOM.closeSuccessModalBtnBottom) {
    DOM.closeSuccessModalBtnBottom.addEventListener('click', () => {
        DOM.orderSuccessBackdrop.classList.remove('open');
    });
}

// ============================================================================
// SALES SUMMARY DASHBOARD (ANALYTICS)
// ============================================================================
async function loadDashboard() {
    if (!canAccessOwnerOrAdmin()) return;
    try {
        const res = await authFetch(API.salesSummary);
        if (!res.ok) return;
        const data = await res.json();
        state.salesSummary = data;

        if (DOM.kpiTotalRevenue) DOM.kpiTotalRevenue.textContent = `₹${Number(data.totalRevenue || 0).toFixed(2)}`;
        if (DOM.kpiTodayRevenue) DOM.kpiTodayRevenue.textContent = `₹${Number(data.todayRevenue || 0).toFixed(2)}`;
        if (DOM.kpiTodayOrdersCount) DOM.kpiTodayOrdersCount.textContent = `${data.todayOrders || 0} order(s) processed today`;
        if (DOM.kpiTotalOrders) DOM.kpiTotalOrders.textContent = data.totalOrders || 0;
        if (DOM.kpiBooksSold) DOM.kpiBooksSold.textContent = data.totalBooksSold || 0;
        if (DOM.kpiAvgOrderValue) DOM.kpiAvgOrderValue.textContent = `₹${Number(data.averageOrderValue || 0).toFixed(2)}`;
    } catch (err) {
        console.warn('Could not load sales summary analytics:', err);
    }
}

// ============================================================================
// OWNER OVERVIEW METRICS (PHASE 11)
// ============================================================================
async function loadOwnerMetrics() {
    if (!canAccessOwnerOrAdmin()) return;
    try {
        const res = await authFetch(API.ownerMetrics);
        if (!res.ok) return;
        const data = await res.json();
        state.ownerMetrics = data;

        if (DOM.metricTotalCatalog) {
            DOM.metricTotalCatalog.textContent = data.totalCatalogSize || 0;
        }
        if (DOM.metricActiveCatalog) {
            DOM.metricActiveCatalog.textContent = `${data.activeCatalogSize || 0} active titles`;
        }
        if (DOM.metricLowStock) {
            DOM.metricLowStock.textContent = data.lowStockCount || 0;
            const tile = document.getElementById('metricLowStockTile');
            if (tile) {
                if ((data.lowStockCount || 0) > 0) {
                    tile.style.borderColor = '#fca5a5';
                    tile.style.background = '#fef2f2';
                } else {
                    tile.style.borderColor = '';
                    tile.style.background = '';
                }
            }
        }
        if (DOM.metricTodaySales) {
            DOM.metricTodaySales.textContent = `₹${Number(data.todaySalesVolume || 0).toFixed(2)}`;
        }
        if (DOM.metricTodayOrders) {
            DOM.metricTodayOrders.textContent = `${data.todayOrders || 0} orders today`;
        }
        if (DOM.metricTotalOrders) {
            DOM.metricTotalOrders.textContent = data.totalOrders || 0;
        }
        if (DOM.metricTotalSales) {
            DOM.metricTotalSales.textContent = `₹${Number(data.totalSalesVolume || 0).toFixed(2)} all-time`;
        }
    } catch (e) {
        console.warn('Could not load owner overview metrics:', e);
    }
}

// ============================================================================
// INVENTORY & CATALOG LIFECYCLE MANAGEMENT (PHASE 11)
// ============================================================================
function initInventoryControls() {
    if (DOM.inventorySearchInput) {
        DOM.inventorySearchInput.addEventListener('input', (e) => {
            state.inventorySearch = e.target.value.trim().toLowerCase();
            if (DOM.clearInventorySearchBtn) {
                DOM.clearInventorySearchBtn.style.display = state.inventorySearch ? 'block' : 'none';
            }
            renderInventoryTable();
        });
    }

    if (DOM.clearInventorySearchBtn) {
        DOM.clearInventorySearchBtn.addEventListener('click', () => {
            DOM.inventorySearchInput.value = '';
            state.inventorySearch = '';
            DOM.clearInventorySearchBtn.style.display = 'none';
            renderInventoryTable();
        });
    }

    if (DOM.invFilterPills) {
        DOM.invFilterPills.forEach(pill => {
            pill.addEventListener('click', () => {
                DOM.invFilterPills.forEach(p => p.classList.remove('active'));
                pill.classList.add('active');
                state.inventoryFilter = pill.getAttribute('data-inv-filter');
                renderInventoryTable();
            });
        });
    }
}

async function loadInventory() {
    if (!canAccessOwnerOrAdmin()) return;
    try {
        const res = await authFetch(API.ownerBooks + '?includeArchived=true');
        if (!res.ok) {
            DOM.inventoryTableBody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 2rem;">Sign in as Owner or Admin to manage inventory.</td></tr>`;
            return;
        }
        const books = await res.json();
        state.inventoryBooks = books;
        renderInventoryTable();
    } catch (e) {
        DOM.inventoryTableBody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--action-danger); padding: 2rem;">Error loading inventory: ${escapeHtml(e.message)}</td></tr>`;
    }
}

function renderInventoryTable() {
    if (!state.inventoryBooks || state.inventoryBooks.length === 0) {
        DOM.inventoryTableBody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 2.5rem;">No inventory titles found in database.</td></tr>`;
        return;
    }

    let filtered = state.inventoryBooks;

    // Status filter
    if (state.inventoryFilter === 'active') {
        filtered = filtered.filter(b => b.active);
    } else if (state.inventoryFilter === 'archived') {
        filtered = filtered.filter(b => !b.active);
    } else if (state.inventoryFilter === 'lowStock') {
        filtered = filtered.filter(b => b.stockCount <= 5 && b.active);
    }

    // Keyword filter
    if (state.inventorySearch) {
        const kw = state.inventorySearch;
        filtered = filtered.filter(b => {
            const titleMatch = b.title && b.title.toLowerCase().includes(kw);
            const pubMatch = b.publisherName && b.publisherName.toLowerCase().includes(kw);
            const genreMatch = b.genre && b.genre.toLowerCase().includes(kw);
            const authorMatch = b.authors && b.authors.some(a => a.toLowerCase().includes(kw));
            return titleMatch || pubMatch || genreMatch || authorMatch;
        });
    }

    if (filtered.length === 0) {
        DOM.inventoryTableBody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 2rem;">No titles match the selected filter.</td></tr>`;
        return;
    }

    DOM.inventoryTableBody.innerHTML = filtered.map(b => {
        const isOutOfStock = b.stockCount <= 0;
        const isLow = b.stockCount <= 5 && b.stockCount > 0;
        let stockBadge = '';
        if (isOutOfStock) {
            stockBadge = `<span class="stock-tag out-of-stock">0 Out</span>`;
        } else if (isLow) {
            stockBadge = `<span class="stock-tag low-stock">Low (${b.stockCount})</span>`;
        } else {
            stockBadge = `<span class="stock-tag in-stock">${b.stockCount}</span>`;
        }

        const statusBadge = b.active
            ? `<span class="status-pill active">Active</span>`
            : `<span class="status-pill archived">Archived</span>`;

        const authors = (b.authors && b.authors.length > 0) ? b.authors.join(', ') : 'Unknown';

        return `
            <tr style="${!b.active ? 'opacity: 0.65; background: #fafafa;' : ''}">
                <td class="num-cell">#${b.bookId}</td>
                <td>
                    <strong>${escapeHtml(b.title)}</strong>
                    <div style="margin-top: 0.15rem;"><span class="genre-tag">${escapeHtml(b.genre || 'General')}</span></div>
                </td>
                <td style="font-size: 0.82rem;">${escapeHtml(authors)}</td>
                <td style="font-size: 0.82rem; color: var(--text-muted);">${escapeHtml(b.publisherName || 'N/A')}</td>
                <td class="num-cell" style="font-weight: 700;">₹${Number(b.price).toFixed(2)}</td>
                <td class="num-cell">${stockBadge}</td>
                <td>${statusBadge}</td>
                <td>
                    <div class="table-actions-cell">
                        <button class="btn btn-secondary btn-xs" onclick="openEditBookModal(${b.bookId})" title="Edit Title Metadata & Authors">
                            Edit
                        </button>
                        <button class="btn btn-secondary btn-xs" onclick="openQuickRestockModal(${b.bookId})" title="Quick Restock Stock Count">
                            Restock
                        </button>
                        <button class="btn btn-ghost btn-xs ${b.active ? 'btn-archive' : 'btn-restore'}" onclick="toggleArchiveBook(${b.bookId})" title="${b.active ? 'Soft-delete / Archive Title' : 'Restore Title'}">
                            ${b.active ? 'Archive' : 'Restore'}
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

// ============================================================================
// QUICK RESTOCK INLINE POPOVER / MODAL
// ============================================================================
function initRestockModal() {
    DOM.closeRestockModalBtn.addEventListener('click', closeRestockModal);
    DOM.cancelRestockBtn.addEventListener('click', closeRestockModal);

    // Delta presets
    document.querySelectorAll('[data-adjust]').forEach(chip => {
        chip.addEventListener('click', () => {
            document.querySelectorAll('[data-adjust]').forEach(c => c.classList.remove('active-chip'));
            chip.classList.add('active-chip');
            const adj = parseInt(chip.getAttribute('data-adjust'), 10);
            DOM.restockAddInput.value = adj;
            updateRestockProjected();
        });
    });

    // Reason tags
    document.querySelectorAll('[data-reason]').forEach(tag => {
        tag.addEventListener('click', () => {
            DOM.restockReasonInput.value = tag.getAttribute('data-reason');
        });
    });

    // Live calculation on input
    DOM.restockAddInput.addEventListener('input', updateRestockProjected);

    DOM.confirmRestockBtn.addEventListener('click', async () => {
        if (!state.selectedBookForRestock) return;
        const delta = parseInt(DOM.restockAddInput.value, 10);
        const reason = DOM.restockReasonInput.value.trim() || 'Inventory Adjustment';

        if (isNaN(delta) || delta === 0) {
            showToast('Please enter a non-zero adjustment delta.', 'error');
            return;
        }

        const currentStock = state.selectedBookForRestock.stockCount || 0;
        if (currentStock + delta < 0) {
            showToast(`Adjustment of ${delta} would cause negative stock (Current: ${currentStock}).`, 'error');
            return;
        }

        try {
            DOM.confirmRestockBtn.disabled = true;
            DOM.confirmRestockBtn.textContent = 'Updating...';

            const res = await authFetch(API.ownerStockAdjust(state.selectedBookForRestock.bookId), {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ adjustment: delta, reason })
            });

            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Failed to adjust stock');

            showToast(`Stock updated for "${state.selectedBookForRestock.title}": ${data.previousStock} -> ${data.currentStock} (${delta >= 0 ? '+' : ''}${delta})`, 'success');
            closeRestockModal();

            // Refresh live inventory, POS catalog, and metrics
            loadInventory();
            loadOwnerMetrics();
            loadCatalog(state.searchQuery, state.currentFilter);

        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            DOM.confirmRestockBtn.disabled = false;
            DOM.confirmRestockBtn.textContent = 'Apply Adjustment';
        }
    });
}

function updateRestockProjected() {
    if (!state.selectedBookForRestock) return;
    const current = state.selectedBookForRestock.stockCount || 0;
    const delta = parseInt(DOM.restockAddInput.value, 10) || 0;
    const projected = current + delta;
    DOM.restockProjectedStock.textContent = projected;
    if (projected < 0) {
        DOM.restockProjectedStock.style.color = 'var(--action-danger)';
    } else if (projected <= 5) {
        DOM.restockProjectedStock.style.color = 'var(--action-warning)';
    } else {
        DOM.restockProjectedStock.style.color = 'var(--action-success)';
    }
}

window.openQuickRestockModal = function(bookId) {
    const book = state.inventoryBooks.find(b => b.bookId === bookId) || state.books.find(b => b.bookId === bookId);
    if (!book) return;

    state.selectedBookForRestock = book;
    DOM.restockBookTitle.textContent = `#${book.bookId} — ${book.title}`;
    DOM.restockCurrentStock.textContent = book.stockCount || 0;
    DOM.restockAddInput.value = 15;
    DOM.restockReasonInput.value = 'Warehouse restock shipment';
    updateRestockProjected();
    DOM.restockModalBackdrop.classList.add('open');
};

window.openRestockModal = window.openQuickRestockModal;

function closeRestockModal() {
    DOM.restockModalBackdrop.classList.remove('open');
    state.selectedBookForRestock = null;
}

// ============================================================================
// PUBLISHER & AUTHOR HELPERS
// ============================================================================
async function loadPublishers() {
    try {
        const res = await authFetch(API.ownerPublishers);
        if (!res.ok) return;
        state.publishers = await res.json();
    } catch (e) {
        console.warn('Could not load publishers:', e);
    }
}

async function loadAuthors() {
    try {
        const res = await authFetch(API.ownerAuthors);
        if (!res.ok) return;
        state.authors = await res.json();
    } catch (e) {
        console.warn('Could not load authors:', e);
    }
}

function populatePublisherSelect(selectElem, selectedId) {
    if (!selectElem) return;
    selectElem.innerHTML = state.publishers.map(p => `
        <option value="${p.publisherId}" ${selectedId && p.publisherId === selectedId ? 'selected' : ''}>
            ${escapeHtml(p.name)} ${p.city ? `(${escapeHtml(p.city)})` : ''}
        </option>
    `).join('');
}

function populateAuthorsCheckboxes(containerElem, selectedIds = []) {
    if (!containerElem) return;
    const selectedSet = new Set(selectedIds.map(Number));
    containerElem.innerHTML = state.authors.map(a => {
        const isChecked = selectedSet.has(Number(a.authorId));
        return `
            <label class="author-chip-label ${isChecked ? 'selected' : ''}" data-author-name="${escapeHtml((a.firstName + ' ' + a.lastName).toLowerCase())}">
                <input type="checkbox" value="${a.authorId}" ${isChecked ? 'checked' : ''} onchange="this.parentElement.classList.toggle('selected', this.checked)">
                <span>${escapeHtml(a.firstName + ' ' + a.lastName)}</span>
            </label>
        `;
    }).join('');
}

// ============================================================================
// ADD NEW TITLE MODAL (PHASE 11)
// ============================================================================
function initAddBookModal() {
    DOM.openAddBookModalBtn.addEventListener('click', async () => {
        await Promise.all([loadPublishers(), loadAuthors()]);
        populatePublisherSelect(DOM.newBookPublisher);
        populateAuthorsCheckboxes(DOM.newBookAuthorsList);
        DOM.newBookTitle.value = '';
        DOM.newBookPrice.value = '499.00';
        DOM.newBookStock.value = '15';
        DOM.newBookGenre.value = 'Software Engineering';
        DOM.inlinePublisherForm.classList.add('hidden');
        DOM.inlineAuthorForm.classList.add('hidden');
        DOM.addBookModalBackdrop.classList.add('open');
    });

    DOM.closeAddBookModalBtn.addEventListener('click', () => {
        DOM.addBookModalBackdrop.classList.remove('open');
    });
    DOM.cancelAddBookBtn.addEventListener('click', () => {
        DOM.addBookModalBackdrop.classList.remove('open');
    });

    // Author search live filter
    if (DOM.authorSearchFilter) {
        DOM.authorSearchFilter.addEventListener('input', (e) => {
            const query = e.target.value.trim().toLowerCase();
            document.querySelectorAll('#newBookAuthorsList .author-chip-label').forEach(chip => {
                const name = chip.getAttribute('data-author-name') || '';
                chip.style.display = (!query || name.includes(query)) ? 'inline-flex' : 'none';
            });
        });
    }

    // Toggle Inline Publisher Form
    DOM.toggleInlinePublisherBtn.addEventListener('click', () => {
        DOM.inlinePublisherForm.classList.toggle('hidden');
    });
    DOM.closeInlinePublisherBtn.addEventListener('click', () => {
        DOM.inlinePublisherForm.classList.add('hidden');
    });

    // Save Inline Publisher
    DOM.saveInlinePubBtn.addEventListener('click', async () => {
        const name = DOM.inlinePubName.value.trim();
        if (!name) {
            showToast('Publisher name is required.', 'error');
            return;
        }
        try {
            DOM.saveInlinePubBtn.disabled = true;
            const res = await authFetch(API.ownerPublishers, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    name,
                    contactNo: DOM.inlinePubPhone.value.trim(),
                    city: DOM.inlinePubCity.value.trim(),
                    street: DOM.inlinePubStreet.value.trim(),
                    pincode: DOM.inlinePubPincode.value.trim()
                })
            });
            const created = await res.json();
            if (!res.ok) throw new Error(created.error || 'Failed to create publisher');

            showToast(`Created publisher "${created.name}"`, 'success');
            await loadPublishers();
            populatePublisherSelect(DOM.newBookPublisher, created.publisherId);
            DOM.inlinePublisherForm.classList.add('hidden');
            DOM.inlinePubName.value = '';
            DOM.inlinePubPhone.value = '';
            DOM.inlinePubCity.value = '';
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            DOM.saveInlinePubBtn.disabled = false;
        }
    });

    // Toggle Inline Author Form
    DOM.toggleInlineAuthorBtn.addEventListener('click', () => {
        DOM.inlineAuthorForm.classList.toggle('hidden');
    });
    DOM.closeInlineAuthorBtn.addEventListener('click', () => {
        DOM.inlineAuthorForm.classList.add('hidden');
    });

    // Save Inline Author
    DOM.saveInlineAuthorBtn.addEventListener('click', async () => {
        const firstName = DOM.inlineAuthorFirstName.value.trim();
        const lastName = DOM.inlineAuthorLastName.value.trim();
        if (!firstName || !lastName) {
            showToast('Author first and last names are required.', 'error');
            return;
        }
        try {
            DOM.saveInlineAuthorBtn.disabled = true;
            const res = await authFetch(API.ownerAuthors, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    firstName,
                    lastName,
                    emailId: DOM.inlineAuthorEmail.value.trim(),
                    biography: DOM.inlineAuthorBio.value.trim()
                })
            });
            const created = await res.json();
            if (!res.ok) throw new Error(created.error || 'Failed to create author');

            showToast(`Created author "${created.firstName} ${created.lastName}"`, 'success');
            await loadAuthors();
            const currentSelected = Array.from(DOM.newBookAuthorsList.querySelectorAll('input:checked')).map(i => parseInt(i.value, 10));
            currentSelected.push(created.authorId);
            populateAuthorsCheckboxes(DOM.newBookAuthorsList, currentSelected);
            DOM.inlineAuthorForm.classList.add('hidden');
            DOM.inlineAuthorFirstName.value = '';
            DOM.inlineAuthorLastName.value = '';
            DOM.inlineAuthorEmail.value = '';
            DOM.inlineAuthorBio.value = '';
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            DOM.saveInlineAuthorBtn.disabled = false;
        }
    });

    // Submit Create Book
    DOM.confirmAddBookBtn.addEventListener('click', async () => {
        const title = DOM.newBookTitle.value.trim();
        const price = parseFloat(DOM.newBookPrice.value);
        const stockCount = parseInt(DOM.newBookStock.value, 10);
        const genre = DOM.newBookGenre.value.trim();
        const publisherId = parseInt(DOM.newBookPublisher.value, 10);

        const selectedAuthorCheckboxes = DOM.newBookAuthorsList.querySelectorAll('input[type="checkbox"]:checked');
        const authorIds = Array.from(selectedAuthorCheckboxes).map(cb => parseInt(cb.value, 10));

        if (!title) {
            showToast('Book title is required.', 'error');
            return;
        }
        if (isNaN(price) || price <= 0) {
            showToast('Price must be greater than zero.', 'error');
            return;
        }
        if (isNaN(stockCount) || stockCount < 0) {
            showToast('Initial stock count cannot be negative.', 'error');
            return;
        }
        if (isNaN(publisherId)) {
            showToast('Please select or create a publisher.', 'error');
            return;
        }
        if (authorIds.length === 0) {
            showToast('Please select at least one author.', 'error');
            return;
        }

        try {
            DOM.confirmAddBookBtn.disabled = true;
            DOM.confirmAddBookBtn.textContent = 'Saving...';

            const res = await authFetch(API.ownerBooks, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    title,
                    price,
                    stockCount,
                    genre,
                    publisherId,
                    authorIds
                })
            });

            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Failed to save book');

            showToast(`Added "${data.title}" to catalog!`, 'success');
            DOM.addBookModalBackdrop.classList.remove('open');

            loadInventory();
            loadOwnerMetrics();
            loadCatalog(state.searchQuery, state.currentFilter);

        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            DOM.confirmAddBookBtn.disabled = false;
            DOM.confirmAddBookBtn.textContent = 'Save New Title';
        }
    });
}

// ============================================================================
// EDIT TITLE MODAL (PHASE 11)
// ============================================================================
function initEditBookModal() {
    DOM.closeEditBookModalBtn.addEventListener('click', () => {
        DOM.editBookModalBackdrop.classList.remove('open');
    });
    DOM.cancelEditBookBtn.addEventListener('click', () => {
        DOM.editBookModalBackdrop.classList.remove('open');
    });

    DOM.confirmEditBookBtn.addEventListener('click', async () => {
        const bookId = parseInt(DOM.editBookId.value, 10);
        const title = DOM.editBookTitle.value.trim();
        const price = parseFloat(DOM.editBookPrice.value);
        const stockCount = parseInt(DOM.editBookStock.value, 10);
        const genre = DOM.editBookGenre.value.trim();
        const publisherId = parseInt(DOM.editBookPublisher.value, 10);
        const active = DOM.editBookStatus.value === 'true';

        const selectedAuthorCheckboxes = DOM.editBookAuthorsList.querySelectorAll('input[type="checkbox"]:checked');
        const authorIds = Array.from(selectedAuthorCheckboxes).map(cb => parseInt(cb.value, 10));

        if (!title || isNaN(price) || isNaN(stockCount)) {
            showToast('Please fill all required book fields.', 'error');
            return;
        }

        try {
            DOM.confirmEditBookBtn.disabled = true;
            DOM.confirmEditBookBtn.textContent = 'Saving...';

            const res = await authFetch(API.ownerBookById(bookId), {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    title,
                    price,
                    stockCount,
                    genre,
                    publisherId,
                    authorIds,
                    active
                })
            });

            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Failed to update book');

            showToast(`Updated "${data.title}" successfully.`, 'success');
            DOM.editBookModalBackdrop.classList.remove('open');

            loadInventory();
            loadOwnerMetrics();
            loadCatalog(state.searchQuery, state.currentFilter);

        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            DOM.confirmEditBookBtn.disabled = false;
            DOM.confirmEditBookBtn.textContent = 'Save Changes';
        }
    });
}

window.openEditBookModal = async function(bookId) {
    const book = state.inventoryBooks.find(b => b.bookId === bookId);
    if (!book) return;

    await Promise.all([loadPublishers(), loadAuthors()]);

    DOM.editBookId.value = book.bookId;
    DOM.editBookModalTitle.textContent = `Edit Title #${book.bookId}: ${book.title}`;
    DOM.editBookTitle.value = book.title;
    DOM.editBookPrice.value = Number(book.price).toFixed(2);
    DOM.editBookStock.value = book.stockCount;
    DOM.editBookGenre.value = book.genre || '';
    DOM.editBookStatus.value = book.active ? 'true' : 'false';

    populatePublisherSelect(DOM.editBookPublisher, book.publisherId);
    populateAuthorsCheckboxes(DOM.editBookAuthorsList, book.authorIds || []);

    DOM.editBookModalBackdrop.classList.add('open');
};

// ============================================================================
// ARCHIVE / RESTORE BOOK
// ============================================================================
window.toggleArchiveBook = async function(bookId) {
    const book = state.inventoryBooks.find(b => b.bookId === bookId);
    if (!book) return;

    const action = book.active ? 'archive' : 'restore';
    const confirmMsg = book.active
        ? `Archive "${book.title}"? It will remain in historical orders but will be hidden from customer POS.`
        : `Restore "${book.title}" to active POS catalog?`;

    if (!confirm(confirmMsg)) return;

    try {
        let res;
        if (book.active) {
            res = await authFetch(API.ownerArchiveBook(bookId), { method: 'DELETE' });
        } else {
            res = await authFetch(API.ownerBookById(bookId), {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ active: true })
            });
        }

        const data = await res.json();
        if (!res.ok) throw new Error(data.error || `Failed to ${action} book`);

        showToast(book.active ? `Archived "${book.title}"` : `Restored "${book.title}" to active catalog`, 'success');
        loadInventory();
        loadOwnerMetrics();
        loadCatalog(state.searchQuery, state.currentFilter);

    } catch (err) {
        showToast(err.message, 'error');
    }
};

// ============================================================================
// ORDERS & RECEIPTS TABLE WITH AUDIT & RESEND RECEIPT (PHASE 11)
// ============================================================================
async function loadOrders() {
    try {
        const isOwnerOrAdmin = canAccessOwnerOrAdmin();
        const endpoint = isOwnerOrAdmin ? API.ownerOrders : API.orders;
        const res = await authFetch(endpoint);
        if (!res.ok) {
            DOM.ordersTableBody.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--text-muted); padding: 2rem;">Sign in to view orders.</td></tr>`;
            return;
        }
        const orders = await res.json();
        state.orders = orders;
        renderOrdersTable();
    } catch (e) {
        DOM.ordersTableBody.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--action-danger); padding: 2rem;">Error loading orders.</td></tr>`;
    }
}

function renderOrdersTable() {
    if (!state.orders || state.orders.length === 0) {
        DOM.ordersTableBody.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--text-muted); padding: 2.5rem;">No settled transaction records found.</td></tr>`;
        return;
    }

    DOM.ordersTableBody.innerHTML = state.orders.map(o => {
        const titles = (o.bookTitles && o.bookTitles.length > 0)
            ? o.bookTitles.join(', ')
            : 'Standard Catalog Items';

        const formattedDate = o.date ? new Date(o.date).toLocaleDateString() + ' ' + new Date(o.date).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) : 'N/A';

        const payMethod = (o.paymentMethod || 'CASH').toUpperCase();
        let payBadgeClass = 'cash';
        if (payMethod.includes('CARD')) payBadgeClass = 'card';
        else if (payMethod.includes('UPI')) payBadgeClass = 'upi';

        return `
            <tr>
                <td class="num-cell">#${o.orderId}</td>
                <td class="num-cell" style="font-size: 0.78rem; color: var(--text-muted);">${formattedDate}</td>
                <td><strong>${escapeHtml(o.customerName || 'Guest')}</strong></td>
                <td style="color: var(--text-muted); font-size: 0.8rem;"><a href="mailto:${escapeHtml(o.customerEmail || '')}" style="color: var(--text-main);">${escapeHtml(o.customerEmail || 'N/A')}</a></td>
                <td><span class="pay-badge ${payBadgeClass}">${payMethod}</span></td>
                <td class="num-cell">${o.quantity}</td>
                <td class="num-cell" style="color: var(--action-success); font-weight: 700;">₹${Number(o.orderAmount).toFixed(2)}</td>
                <td style="font-size: 0.78rem; max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;" title="${escapeHtml(titles)}">${escapeHtml(titles)}</td>
                <td>
                    <div class="table-actions-cell">
                        <button class="btn btn-secondary btn-xs" onclick="resendReceiptEmail(${o.orderId}, this)" title="Resend digital OpenPDF receipt email to customer">
                            ✉️ Resend Receipt
                        </button>
                        <a href="${API.receipt(o.orderId)}" target="_blank" class="btn btn-ghost btn-xs" title="Download OpenPDF invoice">
                            PDF
                        </a>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

window.resendReceiptEmail = async function(orderId, btnElem) {
    if (!orderId) return;
    try {
        if (btnElem) {
            btnElem.disabled = true;
            btnElem.textContent = 'Sending...';
        }

        const res = await authFetch(API.ownerResendReceipt(orderId), { method: 'POST' });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Failed to resend receipt');

        showToast(data.message || `Receipt email resent for Order #${orderId}`, 'success');

    } catch (err) {
        showToast(err.message, 'error');
    } finally {
        if (btnElem) {
            btnElem.disabled = false;
            btnElem.textContent = '✉️ Resend Receipt';
        }
    }
};

// ============================================================================
// IN-STORE SHELF RESERVATIONS & PICKUP HOLDS (RETAIL COMPANION)
// ============================================================================
async function loadHolds() {
    try {
        const isCustomer = state.currentUser && state.currentUser.role === 'CUSTOMER';
        let url = API.holds;
        if (isCustomer && state.currentUser.email) {
            url += `?email=${encodeURIComponent(state.currentUser.email)}`;
        }

        const res = await authFetch(url);
        if (!res.ok) {
            if (DOM.holdsTableBody) {
                DOM.holdsTableBody.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--text-muted); padding: 2rem;">No in-store hold requests found.</td></tr>`;
            }
            return;
        }

        const holds = await res.json();
        state.holds = holds || [];

        // Update badge count with active holds
        const activeCount = state.holds.filter(h => h.status === 'HELD').length;
        if (DOM.holdsCountBadge) {
            DOM.holdsCountBadge.textContent = activeCount;
            DOM.holdsCountBadge.style.display = activeCount > 0 ? 'inline-block' : 'none';
        }

        renderHoldsTable();
    } catch (e) {
        console.warn('Could not load in-store holds:', e);
    }
}

function renderHoldsTable() {
    if (!DOM.holdsTableBody) return;
    if (!state.holds || state.holds.length === 0) {
        DOM.holdsTableBody.innerHTML = `
            <tr>
                <td colspan="9" style="text-align: center; color: var(--text-muted); padding: 2.5rem;">
                    No customer hold reservations currently in queue.
                </td>
            </tr>
        `;
        return;
    }

    const isStaff = canAccessOwnerOrAdmin();

    DOM.holdsTableBody.innerHTML = state.holds.map(h => {
        const isHeld = h.status === 'HELD';
        const formattedCreated = h.createdAt ? new Date(h.createdAt).toLocaleDateString() + ' ' + new Date(h.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) : 'N/A';
        const shelf = h.shelfLocation || 'Main Bay';

        let statusBadgeClass = 'status-pill ';
        if (h.status === 'HELD') statusBadgeClass += 'status-held';
        else if (h.status === 'FULFILLED') statusBadgeClass += 'status-fulfilled';
        else statusBadgeClass += 'status-cancelled';

        let actionBtns = '';
        if (isHeld) {
            if (isStaff) {
                actionBtns = `
                    <div class="table-actions-cell" style="justify-content: center;">
                        <button class="btn btn-primary btn-xs" onclick="fulfillHoldAtCounter(${h.holdId})" title="Load into cashier register to ring up bill">
                            🛎️ Ring Up Bill
                        </button>
                        <button class="btn btn-ghost btn-xs" onclick="cancelHold(${h.holdId})" title="Release book back to shelves">
                            Release
                        </button>
                    </div>
                `;
            } else {
                actionBtns = `
                    <div class="table-actions-cell" style="justify-content: center;">
                        <button class="btn btn-ghost btn-xs" onclick="cancelHold(${h.holdId})" title="Cancel your reservation">
                            Cancel Hold
                        </button>
                    </div>
                `;
            }
        } else {
            actionBtns = `<span style="font-size: 0.78rem; color: var(--text-muted);">${escapeHtml(h.status)}</span>`;
        }

        return `
            <tr>
                <td class="num-cell"><strong>${escapeHtml(h.holdCode)}</strong></td>
                <td><strong>${escapeHtml(h.customerName)}</strong></td>
                <td style="font-size: 0.8rem; color: var(--text-muted);">
                    <div>${escapeHtml(h.customerPhone || '—')}</div>
                    <div>${escapeHtml(h.customerEmail || '—')}</div>
                </td>
                <td>
                    <strong>${escapeHtml(h.bookTitle)}</strong>
                    <div style="font-size: 0.75rem; color: var(--text-muted);">ID: #${h.bookId}</div>
                </td>
                <td>
                    <span class="shelf-location-badge" style="font-size: 0.75rem; padding: 0.15rem 0.45rem;">
                        📍 ${escapeHtml(shelf)}
                    </span>
                </td>
                <td class="num-cell" style="font-weight: 700; color: var(--text-main);">₹${Number(h.price || 0).toFixed(2)}</td>
                <td style="font-size: 0.8rem;">
                    <div>${escapeHtml(h.pickupWindow ? h.pickupWindow.replace('_', ' ') : 'Today')}</div>
                    <div style="font-size: 0.72rem; color: var(--text-muted);">Placed: ${formattedCreated}</div>
                </td>
                <td>
                    <span class="${statusBadgeClass}">${escapeHtml(h.status)}</span>
                </td>
                <td>
                    ${actionBtns}
                </td>
            </tr>
        `;
    }).join('');
}

function openHoldModal(bookId) {
    const book = state.books.find(b => b.bookId === bookId);
    if (!book) return;

    if (book.stockCount <= 0) {
        showToast('This book is currently out of stock on shelves.', 'error');
        return;
    }

    DOM.holdBookId.value = book.bookId;
    const authors = (book.authors && book.authors.length > 0) ? book.authors.join(', ') : 'Unknown Author';
    const shelf = book.shelfLocation || 'Aisle 1, Main Bay';

    DOM.holdBookPreviewCard.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem;">
            <div>
                <span class="shelf-location-badge" style="margin-bottom: 0.35rem; display: inline-block;">📍 ${escapeHtml(shelf)}</span>
                <h4 style="margin: 0.25rem 0 0.15rem; font-weight: 800; font-size: 1.05rem;">${escapeHtml(book.title)}</h4>
                <p style="margin: 0; font-size: 0.8rem; color: var(--text-muted);">by ${escapeHtml(authors)}</p>
            </div>
            <div style="text-align: right;">
                <div style="font-size: 1.1rem; font-weight: 800; color: var(--text-main);">₹${Number(book.price).toFixed(2)}</div>
                <div style="font-size: 0.75rem; color: var(--action-success); font-weight: 600;">In Stock (${book.stockCount})</div>
            </div>
        </div>
    `;

    // Pre-fill user details if logged in
    if (state.currentUser) {
        const u = state.currentUser;
        if (DOM.holdCustName && !DOM.holdCustName.value) {
            DOM.holdCustName.value = u.displayName || (u.email ? u.email.split('@')[0] : '');
        }
        if (DOM.holdCustEmail && !DOM.holdCustEmail.value) {
            DOM.holdCustEmail.value = u.email || '';
        }
    }

    DOM.holdReservationForm.classList.remove('hidden');
    DOM.holdSuccessView.classList.add('hidden');
    DOM.holdModalBackdrop.classList.add('open');
}

function closeHoldModal() {
    DOM.holdModalBackdrop.classList.remove('open');
}

function initHoldModal() {
    if (DOM.closeHoldModalBtn) DOM.closeHoldModalBtn.addEventListener('click', closeHoldModal);
    if (DOM.cancelHoldBtn) DOM.cancelHoldBtn.addEventListener('click', closeHoldModal);
    if (DOM.closeHoldSuccessBtn) DOM.closeHoldSuccessBtn.addEventListener('click', closeHoldModal);
    if (DOM.holdModalBackdrop) {
        DOM.holdModalBackdrop.addEventListener('click', (e) => {
            if (e.target === DOM.holdModalBackdrop) closeHoldModal();
        });
    }

    if (DOM.holdReservationForm) {
        DOM.holdReservationForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const bookId = parseInt(DOM.holdBookId.value, 10);
            const customerName = DOM.holdCustName.value.trim();
            const customerPhone = DOM.holdCustPhone.value.trim();
            const customerEmail = DOM.holdCustEmail.value.trim();
            const pickupWindow = DOM.holdPickupWindow.value;

            if (!customerName || !customerPhone) {
                showToast('Name and Mobile Phone are required for shelf reservation.', 'error');
                return;
            }

            try {
                DOM.confirmHoldBtn.disabled = true;
                DOM.confirmHoldBtn.textContent = 'Reserving Copy...';

                const payload = {
                    bookId,
                    customerName,
                    customerPhone,
                    customerEmail,
                    pickupWindow
                };

                const res = await authFetch(API.holds, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });

                const data = await res.json();
                if (!res.ok) throw new Error(data.error || 'Failed to place hold');

                // Render success hold slip
                DOM.holdReservationForm.classList.add('hidden');
                DOM.holdSuccessView.classList.remove('hidden');

                const exp = data.expiresAt ? new Date(data.expiresAt).toLocaleDateString() + ' ' + new Date(data.expiresAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) : '48 Hours';

                DOM.holdSlipDetails.innerHTML = `
                    <div style="background: var(--bg-alt); padding: 0.85rem; border-radius: var(--radius-sm); border: 1px dashed var(--border-color); text-align: left; font-size: 0.85rem;">
                        <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border-color); padding-bottom: 0.5rem; margin-bottom: 0.5rem;">
                            <span style="font-weight: 800; font-size: 1.1rem; color: var(--action-primary);">${escapeHtml(data.holdCode)}</span>
                            <span class="shelf-location-badge">📍 ${escapeHtml(data.shelfLocation)}</span>
                        </div>
                        <div class="receipt-row"><span>Book Title:</span> <strong>${escapeHtml(data.bookTitle)}</strong></div>
                        <div class="receipt-row"><span>Reserved For:</span> <strong>${escapeHtml(data.customerName)}</strong></div>
                        <div class="receipt-row"><span>Counter Settle Price:</span> <strong style="color: var(--action-success);">₹${Number(data.price || 0).toFixed(2)}</strong></div>
                        <div class="receipt-row"><span>Hold Guaranteed Until:</span> <span>${exp}</span></div>
                        <div style="margin-top: 0.65rem; padding-top: 0.5rem; border-top: 1px solid var(--border-color); font-size: 0.75rem; color: var(--text-muted);">
                            ${escapeHtml(data.instructions || 'Show this Hold Code to the cashier at BookHaven to collect your book.')}
                        </div>
                    </div>
                `;

                showToast(`Reserved! Your Hold Slip code is ${data.holdCode}`, 'success');
                loadHolds();

            } catch (err) {
                showToast(err.message, 'error');
            } finally {
                DOM.confirmHoldBtn.disabled = false;
                DOM.confirmHoldBtn.textContent = 'Confirm In-Store Hold';
            }
        });
    }
}

window.openHoldModal = openHoldModal;

window.fulfillHoldAtCounter = function(holdId) {
    const hold = state.holds.find(h => h.holdId === holdId);
    if (!hold) return;

    const book = state.books.find(b => b.bookId === hold.bookId);
    if (!book) {
        showToast('Book record not found in catalog.', 'error');
        return;
    }

    // Set hold in register
    state.activeHoldFulfillment = hold;

    // Clear cart and put held book
    state.cart.clear();
    state.cart.set(book.bookId, { book, quantity: 1 });

    // Show hold banner in cashier drawer
    if (DOM.registerHoldBanner) {
        DOM.registerHoldBanner.classList.remove('hidden');
        DOM.registerHoldCode.textContent = `${hold.holdCode} (${hold.customerName})`;
    }

    // Auto-select or pre-fill customer
    let matchedCust = state.customers.find(c =>
        (hold.customerEmail && c.emailId && c.emailId.toLowerCase() === hold.customerEmail.toLowerCase())
    );

    if (matchedCust) {
        DOM.newCustomerCheckbox.checked = false;
        DOM.newCustomerFields.classList.add('hidden');
        DOM.customerSelect.disabled = false;
        DOM.customerSelect.value = matchedCust.customerId;
    } else {
        DOM.newCustomerCheckbox.checked = true;
        DOM.newCustomerFields.classList.remove('hidden');
        DOM.customerSelect.disabled = true;
        DOM.custFirstName.value = hold.customerName.split(' ')[0] || hold.customerName;
        DOM.custLastName.value = hold.customerName.split(' ').slice(1).join(' ') || 'Customer';
        DOM.custEmail.value = hold.customerEmail || '';
        DOM.custPhone.value = hold.customerPhone || '';
    }

    // Open register drawer
    openCart();
    showToast(`Loaded hold slip ${hold.holdCode} into register!`, 'info');
};

window.cancelHold = async function(holdId) {
    if (!confirm('Are you sure you want to release this in-store hold?')) return;
    try {
        const res = await authFetch(API.holdCancel(holdId), { method: 'DELETE' });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Failed to cancel hold');

        showToast(data.message || `Hold #${holdId} released.`, 'info');
        loadHolds();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

// ============================================================================
// TOAST NOTIFICATIONS & UTILS
// ============================================================================
function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `<span>${escapeHtml(message)}</span>`;
    DOM.toastContainer.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        toast.style.transition = 'all 0.2s ease-out';
        setTimeout(() => toast.remove(), 200);
    }, 3200);
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
