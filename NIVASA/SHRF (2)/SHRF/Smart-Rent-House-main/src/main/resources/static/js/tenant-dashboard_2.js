// Called by Google Maps callback to indicate library is ready
function initTenantMaps() { window._googleMapsReady = true; }

function openPropertyMap(btn) {
    const item = btn.closest('.property-item');
    if (!item) return;
    const lat = parseFloat(item.getAttribute('data-lat'));
    const lng = parseFloat(item.getAttribute('data-lng'));
    const name = item.getAttribute('data-name') || 'Property Location';
    if (isNaN(lat) || isNaN(lng)) {
        alert('Location not set for this property');
        return;
    }
    showPropertyMap(lat, lng, name);
}

let _modalMap, _modalMarker;
function showPropertyMap(lat, lng, title) {
    const modal = document.getElementById('mapModal');
    const titleEl = document.getElementById('modalTitle');
    titleEl.textContent = title;
    modal.style.display = 'flex';

    // Ensure Google Maps loaded
    if (!window.google || !google.maps) {
        if (!window._googleMapsReady) {
            alert('Map library is loading — try again in a moment.');
            return;
        }
    }

    const mapEl = document.getElementById('modalMap');
    const pos = { lat: parseFloat(lat), lng: parseFloat(lng) };
    if (!_modalMap) {
        _modalMap = new google.maps.Map(mapEl, { center: pos, zoom: 15 });
        _modalMarker = new google.maps.Marker({ position: pos, map: _modalMap });
    } else {
        _modalMap.setCenter(pos);
        _modalMarker.setPosition(pos);
    }
}

document.getElementById('closeMapModal').addEventListener('click', function() {
    document.getElementById('mapModal').style.display = 'none';
});
