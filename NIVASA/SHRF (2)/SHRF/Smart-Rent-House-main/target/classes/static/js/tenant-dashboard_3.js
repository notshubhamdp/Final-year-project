</script>

<!-- FOOTER -->
<footer class="footer">
        <div class="footer-content">
            <div class="footer-section">
                <h4>🏠 Smart Rent House</h4>
                <p style="color:rgba(255,255,255,0.7);margin:8px 0;line-height:1.6">Your trusted platform for finding and listing rental properties in India.</p>
            </div>
            <div class="footer-section">
                <h4>For Tenants</h4>
                <a href="/tenant-dashboard">Browse Properties</a>
                <a href="/favorites">My Favorites</a>
                <a href="#">Saved Searches</a>
                <a href="#">My Bookings</a>
            </div>
            <div class="footer-section">
                <h4>For Landlords</h4>
                <a href="/landlord-dashboard">List Property</a>
                <a href="#">Manage Properties</a>
                <a href="#">Tenant Inquiries</a>
                <a href="#">Payments</a>
            </div>
            <div class="footer-section">
                <h4>Support</h4>
                <a href="#">Help Center</a>
                <a href="#">Contact Us</a>
                <a href="#">Privacy Policy</a>
                <a href="#">Terms & Conditions</a>
            </div>
        </div>
        <div class="footer-bottom">
            <p>&copy; 2025 Smart Rent House. All rights reserved. | Built with ❤️ for renters and landlords</p>
        </div>
    </footer>
<script>
    // Search functionality
    document.getElementById('searchInput').addEventListener('keyup', function() {
        const query = this.value.toLowerCase();
        const items = document.querySelectorAll('.property-item');
        
        items.forEach(item => {
            const name = item.querySelector('.property-name').textContent.toLowerCase();
            const address = item.querySelector('.property-address').textContent.toLowerCase();
            
            if (name.includes(query) || address.includes(query)) {
                item.style.display = '';
            } else {
                item.style.display = 'none';
            }
        });
    });
