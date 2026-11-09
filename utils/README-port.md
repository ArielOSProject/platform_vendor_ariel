# lineage-port.sh: prebacivanje ArielOS commitova na novi LineageOS

Skripta uzima commitove iz `custom-commits.md` i cherry-pick-uje ih (`git cherry-pick -x`, od najstarijeg ka najnovijem) na čist `lineage/<ref>` u svakom projektu iz `lineage-forked-list`. Rezultat je grana `ariel-<ref>` po projektu.

To NIJE `lineage-merge.sh`: ne briše direktorijume, ne radi `repo sync` i ne spaja. Za tvoje commitove na novoj verziji koristi `lineage-port.sh`, a `lineage-merge.sh` je za običan merge upstream-a u postojeću granu.

Ime `<ref>` je grana LineageOS-a, npr. `lineage-24`. U primerima ispod to je `lineage-24`.

## Pre početka

1. Svi projekti treba da su sinhronizovani (`repo sync`) i da nemaju nekomitovan rad. Prljavo radno stablo skripta odbija.
2. Svi tvoji commitovi iz `custom-commits.md` moraju biti dostupni u repo-u (lokalno ili na `ariel` remote-u). Ako hash nije nađen, skripta pokuša `git fetch ariel`.
3. Grana `lineage-24` mora postojati na GitHub-u za svaki projekat. Ako ne postoji, taj projekat se prijavi kao `FETCH FAILED`.
4. Lokalna grana `ariel-lineage-24` ne sme već postojati (npr. od `lineage-merge.sh`), inače skripta staje. Obriši je pa počni: `git -C <projekat> branch -D ariel-lineage-24`.

Pokreće se iz bilo kog direktorijuma (skripta sama nalazi koren stabla):

```
cd vendor/ariel/utils
```

## Tok rada u pet koraka

### 1. Probni prolaz

Ništa ne menja, samo ispiše šta bi se cherry-pick-ovalo.

```
./lineage-port.sh lineage-24 --dry-run
```

Proveri da su svi projekti i commitovi tu i da su u pravom redosledu.

### 2. Prvi pokušaj na jednom malom projektu

```
./lineage-port.sh lineage-24 --only packages/providers/BlockedNumberProvider
```

Za `build` koristi put `build/make`, ne `build`.

### 3. Svi projekti

```
./lineage-port.sh lineage-24
```

Ako svi pick-ovi prođu bez konflikta, na kraju vidiš `Done:` sa svim projektima i spisak novih hash-eva za `custom-commits.md`.

### 4. Konflikt i pregled (opisano ispod)

### 5. Push posle pregleda

```
./lineage-port.sh lineage-24 --push
```

Push ide samo za projekte koji su kompletno prošli (nema konflikta ni nepregledanog rešenja). Bez `--push` se nikad ne push-uje.

## Šta se dešava kad naiđe konflikt

Skripta stane za taj projekat (ostali nastavljaju) i ispiše:

```
 -> [3/7] picking 631fde73b3fc: IntentFirewall: Support blocking ...
    CONFLICT in frameworks/base:
      services/core/java/com/android/server/firewall/IntentFirewall.java
```

Projekat je sad usred cherry-picka. Stanje je zapamćeno u `.port-state/lineage-24/frameworks_base.state`.

### Rešavanje

U direktorijumu projekta:

```
cd frameworks/base
git status                      # vidi fajlove sa konfliktom
# otvori fajlove, reši <<<<<<< ======= >>>>>>> markere
git add services/core/java/com/android/server/firewall/IntentFirewall.java
GIT_EDITOR=true git cherry-pick --continue
```

(Konflikte može da reši i Claude u sesiji, na isti način.)

### Pauza za pregled

Ponovo pokreni skriptu:

```
cd vendor/ariel/utils
./lineage-port.sh lineage-24
```

Skripta prepozna da je konflikt rešen, ali ne nastavlja dalje. Ispiše:

```
 -> PAUSED for review: frameworks/base
    original: 631fde73b3fc   resolved as: a1b2c3d4e5f6
    inspect:  git -C .../frameworks/base show a1b2c3d4e5f6
              less .../.port-state/lineage-24/frameworks_base.review.txt
    approve:  ./lineage-port.sh lineage-24 --approve frameworks/base
```

Pregledaj:

```
less .port-state/lineage-24/frameworks_base.review.txt   # range-diff original vs. rešeno
git -C ../../../frameworks/base show a1b2c3d4e5f6        # sam commit
```

`range-diff` prikazuje samo ono što se razlikuje između tvog originalnog commita i rešenog, što je najbrži način da vidiš šta je menjano pri rešavanju.

### Odobravanje i nastavak

Ako je sve u redu:

```
./lineage-port.sh lineage-24 --approve frameworks/base
./lineage-port.sh lineage-24 --only frameworks/base     # nastavlja od sledećeg commita
```

Za više projekata odjednom: `--approve-all`.

Ako rešenje nije dobro, popravi ga (`git commit --amend` u projektu), pa tek onda odobri.

## Provera stanja

```
./lineage-port.sh lineage-24 --status
```

```
build/make                                    1/1  gate=none
build/soong                                   1/1  gate=none
frameworks/base                               3/7  gate=review
frameworks/opt/telephony                      0/3  gate=none
packages/providers/BlockedNumberProvider      3/3  gate=none
```

`gate` je `none` (slobodno), `conflict` (otvoren konflikt) ili `review` (čeka tvoje odobrenje). Broj levo je koliko je commitova obrađeno od ukupno.

## Preskakanje commitova koje radiš ručno

Neke commitove nema smisla pick-ovati mehanički, na primer `402c51b` (22.2 port u frameworks/base, treba ga prepisati za novu verziju). Stavi ih u `utils/port-skip.txt`:

```
402c51b6d435   # 22.2 port, ručno prepisati za 24
```

Skripta ih u ispisu označi kao `SKIPPED` i ide dalje. Dovoljan je prefiks hash-a. Posle toga commit uradiš ručno u projektu, kad skripta završi ostalo.

Ako uradiš `git cherry-pick --abort` ili `--skip` usred konflikta, skripta to prepozna i isti commit pokuša ponovo pri sledećem pokretanju. Da ga trajno preskočiš, dodaj ga u `port-skip.txt`.

## Ažuriranje custom-commits.md

Kad projekat prođe, skripta na kraju ispiše nove hash-eve u formatu tabele:

```
## frameworks/opt/telephony
| 8f3a1c2 | 2024-10-22 | ArielOS: Send SMS broadcast to Ariel Guardian ... |
```

Prepiši ih u `custom-commits.md` (sekcija tog projekta) i osveži datum snimka u zaglavlju. Hash-evi na novoj grani se razlikuju od starih.

## Početak ispočetka

Za jedan projekat:

```
git -C frameworks/base cherry-pick --abort 2>/dev/null
git -C frameworks/base checkout --detach
git -C frameworks/base branch -D ariel-lineage-24
rm utils/.port-state/lineage-24/frameworks_base.*
```

Za sve: obriši `utils/.port-state/lineage-24/` i granu `ariel-lineage-24` u svakom projektu.

## Prečica: sve opcije

| Komanda | Šta radi |
|---|---|
| `./lineage-port.sh <ref>` | pokreće / nastavlja za sve projekte |
| `--only <putanja>` | samo taj projekat |
| `--dry-run` | ništa ne menja, samo ispiše |
| `--push` | push `ariel-<ref>` na `ariel` remote za završene projekte |
| `--status` | napredak po projektima |
| `--approve <putanja>` / `--approve-all` | odobrava pregledano rešenje konflikta |

Promenljiva okruženja: `SRC_REF` (default `lineage-22.2`) je grana na kojoj su hash-evi iz `custom-commits.md`; koristi se za dohvatanje commitova koji nedostaju lokalno.

## Fajlovi

- `lineage-port.sh`: skripta
- `custom-commits.md`: izvor commitova (tabele po projektu)
- `port-skip.txt`: commitovi koje skripta preskače (ručni rad), opciono
- `.port-state/<ref>/`: stanje i review fajlovi (gitignored)
