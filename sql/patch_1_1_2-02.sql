-- patch-fil för att uppdatera en tabell för att kunna pausa en/flera skördning/ar
ALTER TABLE harvestservices ADD COLUMN paused boolean;

UPDATE harvestservices
SET paused = 'f'