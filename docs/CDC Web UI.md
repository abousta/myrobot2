UI

# Section du haut
  * Affichage et changement de compte
  * Lance le relevé bancaire avec playwright
  * Lance l'ajout des nouvelles lignes dans la bdd

# Section relevés
  * Filtre
    * Date Range
    * Non validés (case à cocher)
    * Les labels qui sont identiques ou qui ressemblent
    * Les tags (on peut en cumuler)
  * Afficher les 100 derniers relevés si aucun filtre précisé 
  * Pouvoir modifier tout champ de la ligne sauf l'id
  * Pouvoir supprimer une ligne après confirmation
  * Afficher les soldes
  * Afficher les tva si compte pro
  * Cliquer sur un label remplit le filtre "label" pour afficher tous les labels qui ressemblent
  * Cliquer sur un tag affiche les lignes qui ont ce tag
  * Pouvoir copier/coller des tags d'une ligne à l'autre
  * Pouvoir noter les lignes non validés comme "validés"

# Section du bas
  * Afficher incohérence de solde pour un compte
  * Pouvoir copier coller la vue pour l'envoyer à Marie (filtrer avec non validés)
  * Afficher la somme des montants de la vue en cours
  * Afficher codes cases à mettre dans la prochaine décla tva et leurs contenus, ainsi qu'un lien vers impots gouv pro

# Page stats
  * A coder plus tard si requêtes sql non suffisantes

# Process hebdo
* Download relevés bancaires avec playwright
* Importer nouvelles lignes dans bdd
  * Ca doit remplir les tags en fonction des labels du passé qui sont identiques ou très proches
  * Ca doit déduire la tva si compte pro
* Mettre les factures dans Tiime
* Remplir / Réajuster les taux de tva du compte pro
* Valider les lignes en changeant éventuellement tags et en cochant validé
