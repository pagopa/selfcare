document.addEventListener('DOMContentLoaded', function() {
        // Seleziona tutti i bottoni con l'attributo data-info
        const buttons = document.querySelectorAll('[data-info]');

        // Aggiungi un event listener per ogni bottone
        buttons.forEach(button => {
            button.addEventListener('click', function() {
                // Ottieni il valore dell'attributo data-info del bottone
                const infoValue = button.getAttribute('data-info');

                // Seleziona il div con l'attributo data-box corrispondente
                const box = document.querySelector(`[data-box="${infoValue}"]`);

                // Alterna la classe active
                if (box.classList.contains('active')) {
                    box.classList.remove('active'); // Rimuove la classe se già presente
                } else {
                    // Rimuovi la classe active da tutti i div
                    const allBoxes = document.querySelectorAll('.messaggio-info');
                    allBoxes.forEach(b => b.classList.remove('active'));

                    // Aggiungi la classe active al div corrispondente
                    box.classList.add('active');
                }
            });
        });

        // Aggiungi un event listener per il pulsante di chiusura
        const closeButtons = document.querySelectorAll('.close-button');
        closeButtons.forEach(button => {
            button.addEventListener('click', function() {
                // Seleziona il div che contiene il pulsante "close"
                const box = button.closest('.messaggio-info');
                
                // Rimuovi la classe active
                if (box) {
                    box.classList.remove('active');
                }
            });
        });
        
         // Seleziona il bottone del menu e il nav
        const menuButton = document.querySelector('.menu-anchor__button');
        const menuNav = document.querySelector('.menu-anchor');
        const menuClose = document.querySelector('.menu-close');

        // Aggiungi un event listener per il clic sul bottone per aprire il menu
        menuButton.addEventListener('click', function() {
            // Aggiungi la classe 'open' al nav e al bottone
            menuNav.classList.add('open');
            menuButton.classList.add('open');
        });

        // Aggiungi un event listener per il clic sulla X per chiudere il menu
        menuClose.addEventListener('click', function() {
            // Rimuovi la classe 'open' dal nav e dal bottone
            menuNav.classList.remove('open');
            menuButton.classList.remove('open');
        });
    });