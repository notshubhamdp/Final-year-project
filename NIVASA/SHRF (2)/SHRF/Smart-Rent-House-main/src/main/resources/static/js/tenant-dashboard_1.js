document.getElementById('searchInput').addEventListener('input', function(e) {
        const query = e.target.value.toLowerCase();
        document.querySelectorAll('.property-item').forEach(item => {
            const name = item.querySelector('.property-name').textContent.toLowerCase();
            const address = item.querySelector('.property-address').textContent.toLowerCase();
            if (name.includes(query) || address.includes(query)) {
                item.style.display = '';
            } else {
                item.style.display = 'none';
            }
        });
    });
