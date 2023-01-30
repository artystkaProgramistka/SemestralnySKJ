Rozproszona baza danych
=======================

Dokumentacja zawierająca opis organizacji sieci, przesyłanych komunikatów i protokołu komunikacji
węzeł-węzeł [tutaj](./DOCUMENTATION.md),
lub w wersji [pdf](./DOCUMENTATION.pdf).
Zostało zaimplementowane i przetestowane wszystko co jest zawarte poleceniu.

Kompilacja i Testowanie
-----------------------

Do uruchamiania testów (`./tests/*.sh`), warto jest używać `tsp` (instalacja na
ubuntu: `sudo apt install task-spooler`).
Poniżej sposób kompilacji i uruchomienia przykładowego testu, tak aby wygodnie było wyświetlać logi z procesów
uruchomionych w tle:

```bash
tsp -K  # terminacja serwera tsp aby numeracja tasków była od 0 
tsp -S 50 # maksymalnie 50 zadań równolegle -- należy zwiększyć w przypadku dodania testów uruchamiających więcej zadań w tle

echo ""
echo "============================================================== compile"
echo ""

javac my_server/NodeConnectionHandler.java my_server/DatabaseNode.java
javac my_server/DatabaseClient.java

echo ""
echo "============================================================== running tests: "
echo ""

# tutaj można odkomentować test który chcemy uruchomić
# bash tests/script-1-0.sh
# bash tests/script-1-1_1.sh
# bash tests/script-1-1_2.sh
# bash tests/script-1-1_3.sh
# bash tests/script-1-1_4.sh
# bash tests/script-2-0_1.sh
# bash tests/script-2-1_1.sh
# bash tests/script-2-1_2.sh
# bash tests/script-2-1_2.sh
# bash tests/script-3-0_1.sh
# bash tests/script-3-0_1.sh
# bash tests/script-3-1_1.sh
bash tests/script-7-1.sh
# bash tests/script-7-2.sh
# bash tests/script-7-p.sh

sleep 1
echo ""
echo "=============================================================="
echo ""
tsp  # wyświetlenie listy procesów uruchomionych w tle

echo ""
echo "============================================================== Output 1:"
echo ""
tsp -c 0  # wyświetlenie stdout+stderr dla pierwszego procesu uruchomionego w tle w danym teście
# ...
```

Szczegółowy opis implementacji
------------------------------

### Ogólna struktura projektu

Zaimplementowane są 3 klasy:

- Klasa klienta `DatabaseClient` -- załączona do treści zadania
- Klasa klienta `DatabaseNode` -- realizująca treść zadania (węzeł uproszczonej rosproszonej bazy danych).
- Pomocnicza (dla `DatabaseNode`) klasa `NodeConnectionHandler` realizuje wysyłanie poleceń innym węzłom -- jej rola
  sprowadza się przede wszystkim do nawiązania połączenia z innym węzłem, nadaniem komunikatu i odebraniu odpowiedzi.

### Główna pętla węzła

W pętli głównej węzła (w metodzie `DatabaseNode.start`), socket serwerowy nasłuchuje na nowe połączenia od zarówno
klientów jak i innych węzłów.

Aby w trakcie obsługi komunikatu mógł dalej przyjmować polecenia (np. ponowny komunikat dotyczący tego samego zapytania
tego samego klienta -- z tym samym `TASK_ID` tylko po
to żeby zwrócić `"ERROR"`),
komunikaty `srv__get-value` , `srv__set-value`, `srv__find-key`, `srv__get-min`, `srv__get-max`, oraz odpowiadające im
komunikaty klienta, są realizowane asynchronicznie (na nowym tymczasowym wątku).

### Opis implementacji protokołu

Opis realizowanych komunikatów węzeł-węzeł jest w [dokumentacji](./DOCUMENTATION.md).
W przypadku każdego komunikatu, protokół sprowadza się do jednorazowego wysłania polecenia i odebraniu
odpowiedzi/wyniku/informacji o niepowodzeniu.

Implementacja komunikatów `srv__get-value`, `srv__set-value`, `srv__find-key`, `srv__get-min`, `srv__get-max` jest
bardzo podobna.
Wszystkie wymagają iteracji po wszystkich połączeniach z innymi węzłami aż do znalezienia i ewentualnie podmiany
wartości,
lub zastosowanie `min` lub `max` na odebranych wynikach (i własnej przetrzymywanej parze klucz-wartość).
Każdy z tych komunikatów dostaje unikalne `TASK_ID`, nadawane przez węzeł który bespośrednio otrzymał odpowiedni
komunikat od klienta (`DatabaseClient`).

Wysyłanie wszystkich komunikatów węzeł-węzeł (`srv__*`) jest owiniętę pomocniczą klasą `NodeConnectionHandler` dla
przejżystości implementacji.

### Szczegółowy opis implementacji najważniejszych metod

- Konstruktor `DatabaseNode` -- przyjmuje sparsowane argumenty z polecenia niezbędne do utworzenia węzła. Konstruje
  połączenia z innymi węzłami, wysyłając `srv__connect` do każdego z nich.
- `DatabaseNode.start` -- zawiera pętlę główną używającą `ServerSocket.accept`. Pętla jest przerywana w wyniku
  otrzymania komunikatu `"terminate"` od klienta (po wcześniejszym wysłaniu komunikatu `srv__disconnect` do połączonych
  bezpośrednio węzłów).
- `DatabaseNode.handleNewSocket` -- implementuje obsługę nowego obiektu `Socket` otrzymaną w metodzie `start`, odczytuje
  i parsuje komunikat, odsyła wynik komunikatu.
- `DatabaseNode.handleNewRequest` -- realizuje poszczególne polecenia -- przyjmuje komunikat i jego argumenty, i zwraca
  odpowiedź/wynik/informację o niepowodzeniu. Niektóre komunikaty realizuje asynchronicznie na oddzielnym tymczasowym
  wątku -- dla przejżystości
  realizacje są owinięte metodą `handleAsyncRequest`.
- `DatabaseNode.getNewTaskId` -- tworzy nowy globalnie unikalny
  napis `"TASK_ID:<licznik_węzła>:<adres_węzła>:<tcpport_węzła>`, oraz inkrementuje licznik zadań węzła (inicjowany
  zerem w momencie skonstruowania `DatabaseNode`).