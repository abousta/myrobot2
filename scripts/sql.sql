-- HEBDO
-- Les derniers relevés qui n'ont pas de tags
select *
from bank_lines
where tags = '[]'
order by date desc;

-- HEBDO
-- Les mouvements pro dont on n'a pas calculé la tva
-- (attention de vérifier le mois plus bas)
select id, date, label, tags, amount, vat
from bank_lines
where (account = 'PRO' or tags like '%Abousta.com%')
  AND vat is null
  AND date like '2026-08-%'
order by date;

-- HEBDO
-- INCONNUS BANCAIRES à envoyer à Marie
select date, label, printf('%.2f EUR', amount / 100.0) AS amount
from bank_lines
where tags = '[]'
order by date;


-- DECLA TVA : A déclarer en case A1 du formulaire 3310 CA3 + case 08 (sans les centimes)
select date, amount, vat, (amount - vat) as ht, (amount - vat) / 100 as to_declare_A1_and_08
from bank_lines
where tags like '%Revenus%'
  and account = 'PRO'
  and date like '2026-07-%';

-- DECLA TVA : Contrôle : doit correspondre au montant reporté à côté de la case 08
select sum(vat) / 100 as control
from bank_lines
where tags like '%Revenus%'
  and account = 'PRO'
  and date like '2026-07-%';

-- DECLA TVA : TVA à déduire : A déclarer en case 20 du formulaire 3310 CA3 (sans les centimes)
select date, tags, vat, sum(vat) / 100 as to_declare_case_20
from bank_lines
where tags not like '%Revenus%'
  and (account = 'PRO' or tags like '%Abousta.com%')
  and vat > 0
  and date like '2026-07-%'
order by date;

-- Les derniers relevés
select *
from bank_lines
order by date desc;

-- Les derniers relevés perso
select *
from bank_lines
where account = 'PERSO'
order by date desc;

-- Les derniers relevés pro
select *
from bank_lines
where (account = 'PRO' or tags like '%Abousta.com%')
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




-- WORKSPACE
select *
from bank_lines
where tags like '%Frais de scolarité%'

select *
from bank_lines
where account = 'PRO'
order by date desc
select *
from bank_lines
where account = 'PRO' and amount = 9240
   or amount = -9240

select *
from bank_lines
where balance is not null
  and account = 'PRO'
order by date desc

select * from bank_lines where label like '%OVH%' order by date desc

update bank_lines set tags='["Lila", "Psychiatre"]' where label like '%BELLET LUCILE%'

select * from bank_lines order by date desc
