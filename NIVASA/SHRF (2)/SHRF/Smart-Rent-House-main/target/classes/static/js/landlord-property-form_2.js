// Update location display helper
function updateLocationDisplay() {
    const lat = document.getElementById('latitude').value;
    const lon = document.getElementById('longitude').value;
    const display = document.getElementById('locationDisplay');
    if (lat && lon) {
        display.textContent = `Latitude: ${lat}, Longitude: ${lon}`;
    } else {
        display.textContent = 'No location selected yet. Click on the map or enter coordinates.';
    }
}

// Google Maps initialization for landlord page
function initLandlordMap() {
    const defaultPos = { lat: 19.0760, lng: 72.8777 };
    const mapEl = document.getElementById('map');
    if (!mapEl) return;

    const map = new google.maps.Map(mapEl, { center: defaultPos, zoom: 12 });
    const marker = new google.maps.Marker({ map: map, draggable: true });

    const latInput = document.getElementById('latitude');
    const lonInput = document.getElementById('longitude');

    // If inputs already have coordinates (editing existing property), show marker
    if (latInput.value && lonInput.value) {
        const pos = { lat: parseFloat(latInput.value), lng: parseFloat(lonInput.value) };
        marker.setPosition(pos);
        map.setCenter(pos);
        map.setZoom(15);
        updateLocationDisplay();
    }

    function placeMarker(latLng) {
        marker.setPosition(latLng);
        latInput.value = latLng.lat().toFixed(6);
        lonInput.value = latLng.lng().toFixed(6);
        updateLocationDisplay();
    }

    map.addListener('click', function(e) { placeMarker(e.latLng); });
    marker.addListener('dragend', function(e) { placeMarker(e.latLng); });

    // Autocomplete: bind to address input so selecting an address centers map
    try {
        const addressInput = document.getElementById('address');
        const autocomplete = new google.maps.places.Autocomplete(addressInput);
        autocomplete.bindTo('bounds', map);
        autocomplete.addListener('place_changed', function() {
            const place = autocomplete.getPlace();
            if (!place.geometry) return;
            map.setCenter(place.geometry.location);
            map.setZoom(17);
            placeMarker(place.geometry.location);
        });
    } catch (e) {
        // places library may not be available, ignore
        console.warn('Places autocomplete not available:', e);
    }
}
