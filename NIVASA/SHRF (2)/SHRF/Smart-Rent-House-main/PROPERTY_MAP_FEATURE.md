# Landlord Property Location Map Feature

## Overview
The Property Location Map feature allows landlords to easily locate and set their property's coordinates on an interactive map. This helps tenants find properties and provides accurate location data for the platform.

## Features

### 1. **Interactive Map Display**
- Uses Leaflet.js (open-source mapping library)
- Integrated with OpenStreetMap for map tiles
- Real-time coordinate display
- Drag-and-drop marker placement

### 2. **Location Search**
- Address-based search using Nominatim (OpenStreetMap's geocoding service)
- Automatic location detection
- Multiple search results support

### 3. **Coordinate Management**
- Manual coordinate entry (Latitude/Longitude)
- Automatic coordinate updates from map interactions
- Validation for valid coordinate ranges:
  - Latitude: -90 to 90
  - Longitude: -180 to 180

### 4. **Landlord Integration**
- Accessible from property details page
- Permission-based access (landlords can only modify their own properties)
- Location updates persist to database
- Visual feedback on successful updates

## Implementation Details

### Database
The `Property` entity already contains:
```java
@Column(name = "latitude")
private Double latitude;

@Column(name = "longitude")
private Double longitude;
```

### Controller Endpoints

#### 1. Display Map Page
**URL:** `/landlord/property-map/{id}`
**Method:** GET
**Purpose:** Display the interactive map interface for a specific property

**Controller Method:**
```java
@GetMapping("/property-map/{id}")
public String showPropertyMap(@PathVariable Long id,
                               Authentication authentication,
                               Model model,
                               RedirectAttributes redirectAttributes)
```

#### 2. Update Property Location
**URL:** `/landlord/property/{id}/update-location`
**Method:** POST
**Parameters:**
- `latitude` (Double): Property latitude
- `longitude` (Double): Property longitude

**Validation:**
- User must be the property owner
- Coordinates must be within valid ranges
- Both latitude and longitude must be provided

**Response:** Redirect to property details page with success/error message

### Templates

#### landlord-property-map.html
Main map interface with:
- Interactive Leaflet map (400px height, responsive)
- Search box for address lookup
- Coordinate display cards
- Form to update and save location
- Back button to return to property details

#### landlord-property-detail.html
Updated with:
- New "Property Location" section
- "View on Map" button to access the map page

### Client-Side Features

**JavaScript Functionality:**
1. **Map Initialization**
   - Centers on property coordinates (if available)
   - Default center: India (20.5937°N, 78.9629°E)

2. **Marker Management**
   - Click on map to place marker
   - Drag marker to update coordinates
   - Automatic coordinate field updates

3. **Search Integration**
   - Enter address and press Enter to search
   - Uses Nominatim API for geocoding
   - Automatically centers map on search result

4. **Coordinate Sync**
   - Manual coordinate entry updates map
   - Map interactions update form fields
   - Real-time coordinate display

### External Dependencies (CDN-based)
- **Leaflet.js:** `https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js`
- **Leaflet CSS:** `https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css`
- **Geocoder:** `https://cdnjs.cloudflare.com/ajax/libs/leaflet-geocoder-nominatim/1.7.1/`
- **OpenStreetMap Tiles:** `https://tile.openstreetmap.org/`

## Usage Flow

### For Landlords

1. **Navigate to Property Details**
   - Go to "My Properties" in landlord dashboard
   - Click on a property to view details

2. **Access Map Interface**
   - Click "📍 View on Map" button
   - Interactive map will display

3. **Set Property Location - Method 1: Click on Map**
   - Click anywhere on the map
   - Marker appears at clicked location
   - Coordinates update automatically

4. **Set Property Location - Method 2: Search Address**
   - Enter address/landmark in search box
   - Press Enter or click search
   - Map centers on location
   - Marker is placed automatically

5. **Set Property Location - Method 3: Manual Coordinates**
   - Enter latitude in first field
   - Enter longitude in second field
   - Map updates to show location

6. **Fine-Tune with Marker Drag**
   - Drag the marker to adjust location
   - Coordinates update in real-time

7. **Save Location**
   - Click "💾 Save Location" button
   - Location is saved to database
   - Returns to property details page
   - Success message displayed

## Validation & Error Handling

### Validation Rules
1. **User Authorization**
   - Must be logged in as landlord
   - Can only modify own properties
   - Returns 403 Forbidden if unauthorized

2. **Coordinate Validation**
   - Latitude: -90.0 to 90.0
   - Longitude: -180.0 to 180.0
   - Both required for update
   - Returns error if invalid

3. **Property Existence**
   - Property must exist in database
   - Landlord ID must match user ID
   - Returns error if property not found

### Error Handling
- Try-catch blocks for exception handling
- Logging of errors with stack traces
- User-friendly error messages
- Redirect to previous page on errors

## Security Considerations

1. **Authentication Required**
   - Spring Security enforces login requirement
   - Anonymous users cannot access map

2. **Permission Checks**
   - Landlords can only access their own properties
   - Database checks ensure ownership

3. **CSRF Protection**
   - CSRF token required for POST requests
   - Thymeleaf template includes token automatically

4. **Input Validation**
   - Server-side coordinate validation
   - Prevents invalid data storage

## Styling

### CSS Files Used
- `landlord-property-map.css`: Map-specific styles
- Inline styles in HTML for quick customization

### Color Scheme
- Primary: #4f46e5 (Indigo)
- Success: #10b981 (Green)
- Danger: #ef4444 (Red)
- Muted: #6b7280 (Gray)

### Responsive Design
- Mobile-friendly layout
- Map height adjusts for smaller screens
- Touch-friendly controls
- Flexible button layout

## Future Enhancements

1. **Multiple Locations Per Property**
   - Support for multiple marked locations
   - Primary location selection

2. **Radius/Area Selection**
   - Property boundary drawing
   - Service area radius

3. **Street View Integration**
   - Google Street View or similar
   - 360° property view

4. **Route Calculation**
   - Distance to landmarks
   - Commute time estimates

5. **Offline Map Support**
   - Download offline maps
   - Use when internet unavailable

6. **Heat Map Display**
   - Popular property areas
   - Demand visualization

## File Locations

### Backend
- Controller: `src/main/java/com/SRHF/SRHF/controller/LandlordPropertyController.java`
- Entity: `src/main/java/com/SRHF/SRHF/entity/Property.java`

### Frontend
- HTML Template: `src/main/resources/templates/landlord-property-map.html`
- Updated Template: `src/main/resources/templates/landlord-property-detail.html`
- CSS Styles: `src/main/resources/static/css/landlord-property-map.css`

## Testing Checklist

- [ ] Landlord can access map from property details
- [ ] Map displays correctly with proper zoom level
- [ ] Clicking map places marker accurately
- [ ] Dragging marker updates coordinates
- [ ] Address search works correctly
- [ ] Manual coordinate entry updates map
- [ ] Save button updates database
- [ ] Error messages display properly
- [ ] Non-owner cannot access other's property map
- [ ] Coordinates validate correctly
- [ ] Page is responsive on mobile devices
- [ ] Navigation back to details works

## Troubleshooting

### Map Not Loading
- Check internet connection for CDN resources
- Verify browser allows JavaScript
- Check browser console for errors

### Marker Not Appearing
- Ensure valid coordinates
- Check zoom level (may need to zoom out)
- Clear browser cache and reload

### Search Not Working
- Verify internet connection
- Check Nominatim service status
- Try different address format

### Coordinates Not Saving
- Verify user is logged in as correct landlord
- Check coordinate format (decimal numbers)
- Verify coordinates are within valid ranges

---

**Last Updated:** January 7, 2026
**Version:** 1.0
