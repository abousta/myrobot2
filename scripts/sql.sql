-- Les derniers relevés
select *
from bank_lines
order by date desc;

-- Les derniers relevés qui n'ont pas de tags
select *
from bank_lines
where tags = '[]'
order by date desc;

-- Urssaf par an
select strftime('%Y', date) as year, sum(amount) / 100 as amount
from bank_lines
where tags like '%urssaf%'
group by year;

-- Revenu par an
select strftime('%Y', date) as year, sum(amount) / 1.2 / 100 as amount
from bank_lines
where tags like '%"Abousta.com","Revenus"%'
  and account = 'compte_pro'
group by year
order by year;

-- Les mouvements pro dont on n'a pas calculé la tva
select id, date, label, tags, amount, vat
from bank_lines
where (account = 'PRO' or tags like '%Abousta.com%')
  AND vat is null
  AND date like '2026-07-%'
order by date;

-- DECLA TVA : A déclarer en case A1 du formulaire 3310 CA3 + case 08 (sans les centimes)
select date, tags, amount, vat, (amount-vat) as ht, (amount-vat)/100 as to_declare from bank_lines where tags like '%Revenus%' and account='PRO' and date like '2026-07-%';

-- DECLA TVA : Contrôle : doit correspondre au montant reporté à côté de la case 08
select sum(vat)/100 as control from bank_lines where tags like '%Revenus%' and account='PRO' and date like '2026-07-%';

-- DECLA TVA : TVA à déduire : A déclarer en case 20 du formulaire 3310 CA3 (sans les centimes)
select sum(vat)/100 as to_declare, date, account, label, tags, vat from bank_lines where tags not like '%Revenus%' and (account = 'PRO' or tags like '%Abousta.com%') and vat>0 and date like '2026-07-%' order by date;

