// Store image state per property to avoid session conflicts
const imageStates = {};

function getPropertyImageState(propertyId) {
    if (!imageStates[propertyId]) {
        imageStates[propertyId] = {
            currentIndex: 0,
            totalImages: 1
        };
    }
    return imageStates[propertyId];
}

function changeMainImage(imgElement, index) {
    document.querySelectorAll('.thumbnail').forEach(t => t.classList.remove('active'));
    imgElement.classList.add('active');
    const propertyId = /*[[${property.id}]]*/ 0;
    const state = getPropertyImageState(propertyId);
    document.getElementById('mainImage').src = '/tenant/file/image/' + propertyId + '/' + index;
    state.currentIndex = index;
    updateCounter(propertyId);
}

function changePropertyDetailImage(btn, direction) {
    const gallery = btn.closest('.gallery');
    const propertyId = gallery.getAttribute('data-property-id');
    const imageCount = parseInt(gallery.getAttribute('data-image-count')) || 1;
    const mainImg = gallery.querySelector('#mainImage');
    const counter = gallery.querySelector('.image-counter');
    const state = getPropertyImageState(propertyId);
    
    // Calculate next index
    let nextIndex = state.currentIndex + direction;
    
    // Wrap around
    if (nextIndex < 0) {
        nextIndex = imageCount - 1;
    } else if (nextIndex >= imageCount) {
        nextIndex = 0;
    }
    
    // Update main image
    mainImg.src = '/tenant/file/image/' + propertyId + '/' + nextIndex;
    mainImg.setAttribute('data-index', nextIndex);
    state.currentIndex = nextIndex;
    
    // Update thumbnail active state
    const thumbnails = gallery.querySelectorAll('.thumbnail');
    thumbnails.forEach((thumb, idx) => {
        if (idx === nextIndex) {
            thumb.classList.add('active');
        } else {
            thumb.classList.remove('active');
        }
    });
    
    // Update counter
    updateCounter(propertyId);
}

function updateCounter(propertyId) {
    const counterEl = document.querySelector('.image-counter');
    if (counterEl) {
        const gallery = counterEl.closest('.gallery');
        const imageCount = parseInt(gallery.getAttribute('data-image-count')) || 1;
        const state = getPropertyImageState(propertyId);
        counterEl.textContent = (state.currentIndex + 1) + '/' + imageCount;
    }
}

function openFullscreen(index) {
    const modal = document.getElementById('fullscreenModal');
    const img = document.getElementById('fullscreenImage');
    const gallery = document.querySelector('.gallery');
    const propertyId = gallery.getAttribute('data-property-id');
    const imageCount = parseInt(gallery.getAttribute('data-image-count')) || 1;
    const state = getPropertyImageState(propertyId);
    
    state.currentIndex = index;
    state.totalImages = imageCount;
    img.src = '/tenant/file/image/' + propertyId + '/' + index;
    updateFullscreenCounter(propertyId);
    
    modal.classList.add('active');
    document.body.style.overflow = 'hidden';
}

function closeFullscreen() {
    const modal = document.getElementById('fullscreenModal');
    modal.classList.remove('active');
    document.body.style.overflow = 'auto';
}

function changeFullscreenImage(direction) {
    const gallery = document.querySelector('.gallery');
    const propertyId = gallery.getAttribute('data-property-id');
    const img = document.getElementById('fullscreenImage');
    const imageCount = parseInt(gallery.getAttribute('data-image-count')) || 1;
    const state = getPropertyImageState(propertyId);
    
    // Calculate next index
    let nextIndex = state.currentIndex + direction;
    
    // Wrap around
    if (nextIndex < 0) {
        nextIndex = imageCount - 1;
    } else if (nextIndex >= imageCount) {
        nextIndex = 0;
    }
    
    // Update fullscreen image
    img.src = '/tenant/file/image/' + propertyId + '/' + nextIndex;
    state.currentIndex = nextIndex;
    state.totalImages = imageCount;
    updateFullscreenCounter(propertyId);
}

function updateFullscreenCounter(propertyId) {
    const counter = document.getElementById('fullscreenCounter');
    if (counter) {
        const state = getPropertyImageState(propertyId);
        counter.textContent = (state.currentIndex + 1) + '/' + state.totalImages;
    }
}

// Close fullscreen on Escape key
document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
        closeFullscreen();
    }
    if (event.key === 'ArrowLeft') {
        const modal = document.getElementById('fullscreenModal');
        if (modal.classList.contains('active')) {
            changeFullscreenImage(-1);
        }
    }
    if (event.key === 'ArrowRight') {
        const modal = document.getElementById('fullscreenModal');
        if (modal.classList.contains('active')) {
            changeFullscreenImage(1);
        }
    }
});

    let propertyLeafletMap;

    function initPropertyMap() {
        const mapEl = document.getElementById('map');
        if (!mapEl) return;

        const lat = parseFloat(mapEl.getAttribute('data-lat'));
        const lng = parseFloat(mapEl.getAttribute('data-lng'));
        const hasCoords = !(isNaN(lat) || isNaN(lng) || (lat === 0 && lng === 0));

        if (!hasCoords) {
            mapEl.innerHTML = '<div style="padding:24px;text-align:center;color:#666">Location not available for this property.</div>';
            return;
        }

        if (typeof L === 'undefined') {
            mapEl.innerHTML = '<div style="padding:24px;text-align:center;color:#666">Map failed to load. Please refresh the page.</div>';
            return;
        }

        if (propertyLeafletMap) {
            propertyLeafletMap.remove();
            propertyLeafletMap = null;
        }

        propertyLeafletMap = L.map(mapEl).setView([lat, lng], 15);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; OpenStreetMap contributors',
            maxZoom: 19,
            maxNativeZoom: 18
        }).addTo(propertyLeafletMap);
        L.marker([lat, lng]).addTo(propertyLeafletMap);
    }

    function openDirections() {
        const mapEl = document.getElementById('map');
        const lat = parseFloat(mapEl.getAttribute('data-lat'));
        const lng = parseFloat(mapEl.getAttribute('data-lng'));
        if (isNaN(lat) || isNaN(lng)) { alert('Property location not available'); return; }
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(function(pos) {
                const userLat = pos.coords.latitude;
                const userLng = pos.coords.longitude;
                const url = `https://www.google.com/maps/dir/?api=1&origin=${userLat},${userLng}&destination=${lat},${lng}`;
                window.open(url, '_blank');
            }, function() {
                const url = `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;
                window.open(url, '_blank');
            });
        } else {
            const url = `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}`;
            window.open(url, '_blank');
        }
    }

    // Initialize total images count on page load
    document.addEventListener('DOMContentLoaded', function() {
        const gallery = document.querySelector('.gallery');
        if (gallery) {
            totalImages = parseInt(gallery.getAttribute('data-image-count')) || 1;
        }
    });

    window.addEventListener('load', initPropertyMap);

window.initPropertyMapCallback = function() {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initPropertyMap);
    } else {
        initPropertyMap();
    }
};
