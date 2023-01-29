Sposób organizacji sieci
------------------------

Każdy węzeł sieci nasłuchuje na jednym porcie (zadanmym parametrem `--tcpport`) w nieskończonej pętli, aż do otrzymania
komunikatu `terminate` od klienta.
Połączenia z innymi węzłami są realizowante poprzez posiadanie słownika `HashMap<String, NodeConnectionHandler>` przez
każdy z węzłów.
Jedna para `<String, NodeConnectionHandler>` reprezentuje połączenie z innym węzłem -- klucz przyjmuje
wartość`adres:tcpport` aby jednoznacznie identyfikować inny węzeł w seici.
Przy terminacji, węzeł wysyła innym węzłom informacje o zakończeniu swojego działania (`srv__disconnect`) aby inne węzły
mogły usunąć go ze swojego słownika połączeń.
Słowniki realizują tym sposobem nieskierowany (połączenia są zawsze w obie strony -- po jednym `NodeConnectionHandler` z
każdej strony) graf połączeń węzłów.

Aby uniknąć cyklów w realizowaniu zadań rozproszonych -- zadaniom są nadawane unikalne `TASK_ID`, w
postaci `TASK_ID:nodeTaskIdCounter:adres_węzła:tcpport`.
W ten sposób węzły mogą zwrócić wiadomość zwrotną`"ERROR"` w przypadku napotkania prośby o realizacje tego samego
zadania (np. `srv__get-min`).

Aby w trakcie obsługi komunikatu mógł dalej przyjmować polecenia (np. ponowne zapytanie z tym samym `TASK_ID` tylko po
to żeby zwrócić`"ERROR"`),
komunikaty `srv__get-value` , `srv__set-value`, `srv__find-key`, `srv__get-min`, `srv__get-max`, oraz odpowiadające im
komunikaty klienta, są realizowane asynchronicznie (na nowym tymczasowym wątku).

Sposób organizacji kodu
-----------------------

Zaimplementowane są 3 klasy:

- Klasa klienta `DatabaseClient` -- załączona do treści zadania
- Klasa klienta `DatabaseNode` -- realizująca treść zadania (węzeł uproszczonej rosproszonej bazy danych).
- Pomocnicza (dla `DatabaseNode`) klasa `NodeConnectionHandler` realizuje wysyłanie poleceń innym serwerom -- jej rola
  sprowadza się przede wszystkim do nawiązania połączenia z innym węzłem, nadaniem komunikatu i odebraniu odpowiedzi.

Przesyłane komunikaty w komunikacji klient-węzeł
------------------------------------------------

Komunikaty są takie jak opisane w treści zadania.
Komunikacja odpywa się poprzez protokół TCP, przy użyciu klas `ServerSocket` oraz `Socket` z biblioteki `java.net`.


Przesyłane komunikaty w komunikacji węzeł-węzeł
-----------------------------------------------

Wszystkie komunikaty węzeł-węzeł zawierają parametr `<myAddress:myTcpPort>` -- przekazuje on adres oraz port
nasłuchiwania węzła nadającego,
służy on do jednoznaczej identyfikacji węzła nadającego oraz aby węzeł zapamiętał jego adres oraz port nasłuchiwania w
przypadku polecenia `srv__connect`.
Unikalny `<taskId>` jest nadawany w niektórych komunikatach, aby uniknąć nieskończonego oczekiwania w przypadku gdy graf
węzłów zawiera cykl.

Pełna lista komunikatów węzeł-węzeł:

- `srv__connect <myAddress:myTcpPort>` -- Nawiązanie połączenia z nowym węzłem który odpowiada `"OK"`. Jako parametr są
  podane adres oraz port serwera ubiegającego o połączenie.
- `srv__disconnect <myAddress:myTcpPort>` -- Informacja o terminacji siebie -- oczekiwana odpowiedź to `"OK"`.
- `srv__get-value <taskId> <key> <myAddress:myTcpPort>` -- Realizacja operacji `get-value` (części związanej z
  komunikacjią węzeł-węzeł) opisanej w zadaniu. Parametr `key` odpowiada parametrowi komunikatu `get-value` klienta.
  Oczekiwana odpowiedź to `"ERROR"` w przypadku wykrycia cyklu komunikatów dla danego `<taskID>` lub w przypadku nie
  odnalezienia danego klucza w sieci węzłów.
- `srv__set-value <taskId> <key:value> <myAddress:myTcpPort>` -- analogicznie jak `srv__get-value`, tylko realizuje
  komunikat `set-value` jak opisane poleceniu. Odpowiedź to szukane `adres:tcpport` lub `"ERROR"`.
- `srv__find-key <taskId> <key> <myAddress:myTcpPort>` -- analogicznie, z tym że realizuje komunikat `find-key` jak
  opisane w poleceniu.
- `srv__get-min <taskId> <myAddress:myTcpPort>` -- analogicznie, z tym że ma tylko 2 argumenty jako że `get-min` nie ma
  dodatkowego argumentu.
- `srv__get-max <taskId> <myAddress:myTcpPort>` -- analogicznie jak `srv__get-min`.

Testowanie
----------

Do uruchamiania testów (`./tests/*.sh`), warto jest używać `tsp` (instalacja na
ubuntu: `sudo apt install task-spooler`).
Poniżej sposób uruchamiania przykładowego testu, tak aby wygodnie było wyświetlać logi z procesów uruchomionych w tle:

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
