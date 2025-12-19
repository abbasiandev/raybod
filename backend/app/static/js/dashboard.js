/**
 * Sentinel Dashboard JavaScript
 */

// Toggle sidebar on mobile
function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    if (sidebar) {
        sidebar.classList.toggle('open');
    }
}

// Close sidebar when clicking outside on mobile
document.addEventListener('click', function(e) {
    const sidebar = document.getElementById('sidebar');
    const menuBtn = document.querySelector('.mobile-menu-btn');
    
    if (sidebar && sidebar.classList.contains('open')) {
        if (!sidebar.contains(e.target) && !menuBtn.contains(e.target)) {
            sidebar.classList.remove('open');
        }
    }
});

// HTMX after swap event for modals
document.body.addEventListener('htmx:afterSwap', function(event) {
    // Re-initialize any components after HTMX swap
});

// Confirm delete actions
function confirmDelete(message) {
    return confirm(message || 'Are you sure you want to delete this item?');
}

// Format numbers with commas
function formatNumber(num) {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

// Initialize charts if Chart.js is loaded
function initChart(canvasId, type, data, options = {}) {
    const canvas = document.getElementById(canvasId);
    if (!canvas || typeof Chart === 'undefined') return null;
    
    const defaultOptions = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: {
                labels: {
                    color: '#E6EDF3',
                    font: {
                        family: '-apple-system, BlinkMacSystemFont, sans-serif'
                    }
                }
            }
        },
        scales: type !== 'pie' && type !== 'doughnut' ? {
            x: {
                grid: {
                    color: 'rgba(48, 54, 61, 0.5)'
                },
                ticks: {
                    color: '#8B949E'
                }
            },
            y: {
                grid: {
                    color: 'rgba(48, 54, 61, 0.5)'
                },
                ticks: {
                    color: '#8B949E'
                }
            }
        } : undefined
    };
    
    return new Chart(canvas, {
        type: type,
        data: data,
        options: { ...defaultOptions, ...options }
    });
}

// Theme color palette for charts
const chartColors = {
    primary: '#0066CC',
    cyan: '#00E5FF',
    pink: '#FF0080',
    purple: '#BF00FF',
    green: '#00E676',
    orange: '#FF9100',
    red: '#FF1744',
    gray: '#8B949E'
};

// Close modal function
function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.remove();
    }
}

// Show toast notification
function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed;
        bottom: 20px;
        right: 20px;
        padding: 1rem 1.5rem;
        background: var(--surface-elevated);
        border: 1px solid var(--border);
        border-radius: var(--radius-md);
        color: var(--text-primary);
        box-shadow: var(--shadow-lg);
        z-index: 1000;
        animation: slideIn 0.3s ease;
    `;
    
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// Add CSS animation
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from { transform: translateX(100%); opacity: 0; }
        to { transform: translateX(0); opacity: 1; }
    }
    @keyframes slideOut {
        from { transform: translateX(0); opacity: 1; }
        to { transform: translateX(100%); opacity: 0; }
    }
`;
document.head.appendChild(style);
