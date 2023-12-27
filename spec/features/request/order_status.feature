Feature: Order status

  Background:
    Given there is an initial admin
    And there are procurement settings

    And there is a budget period "Procuring" in past phase
    And there is a budget period "Inspecting" in inspection phase
    And there is a budget period "Requesting" in requesting phase

    And there is a main category "Tiere"
    And there is category "Säugetiere" for main category "Tiere"
    And there is an inspector for category "Säugetiere"
    And there is a viewer for category "Säugetiere"

    And there is a department "Zoo"
    And there is an organization "Tiere der Savanne" within "Zoo"
    And there is a requester "Elvira Ehring" for "Tiere der Savanne" within "Zoo"

    And there is a request with the following data:
      | field                | value             |
      | Requester            | Elvira Ehring     |
      | Organization         | Tiere der Savanne |
      | Budgetperiode        | Procuring         |
      | Kategorie            | Säugetiere        |
      | Artikel oder Projekt | Elefant           |
      | Menge beantragt      | 1                 |
      | Menge bewilligt      | 1                 |
      | Priorität            | Normal            |
    And there is a request with the following data:
      | field                | value             |
      | Requester            | Elvira Ehring     |
      | Organization         | Tiere der Savanne |
      | Budgetperiode        | Inspecting        |
      | Kategorie            | Säugetiere        |
      | Artikel oder Projekt | Zebra             |
      | Menge beantragt      | 1                 |
      | Priorität            | Normal            |
    And there is a request with the following data:
      | field                | value             |
      | Requester            | Elvira Ehring     |
      | Organization         | Tiere der Savanne |
      | Budgetperiode        | Requesting        |
      | Kategorie            | Säugetiere        |
      | Artikel oder Projekt | Gepard            |
      | Menge beantragt      | 1                 |
      | Priorität            | Normal            |

  Scenario: Inspector can view and edit the order status
    When I log in as the inspector

    Then I see "3 Anträge"
    And "Status Beschaffung" filter has following checkboxes:
      | Nicht bearbeitet      |
      | In Bearbeitung        |
      | Beschafft             |
      | Alternative beschafft |
      | Nicht beschafft       |

    # Focus on the budget period which typically has items to be procured
    When I uncheck all items for "Budgetperioden" filter
    And I check "Procuring" for "Budgetperioden" filter
    Then I see "1 Antrag"

    # Filter: There must be exactly 1 item in state "Nicht bearbeitet" (= initial state)
    When I uncheck "Nicht bearbeitet" for "Status Beschaffung" filter
    Then I see "0 Anträge"
    And I check "Nicht bearbeitet" for "Status Beschaffung" filter
    And I see "1 Antrag"

    # Open item form and check order status
    And I expand the line of the main category "Tiere"
    And I expand the line of the category "Säugetiere"
    And I expand the line of the request for "Elefant"
    Then the request form has the following data:
      | field                  | value            |
      | Beschaffungs-Status    | Nicht bearbeitet |
      | Beschaffungs-Kommentar |                  |

    # Modify order status
    When I enter the following data into the request form:
      | field                  | value                         |
      | Beschaffungs-Status    | Beschafft                     |
      | Beschaffungs-Kommentar | Wurde bestellt und geliefert! |
    And I click on 'Speichern'

    # Make sure filter reflects the new status
    And Sleep 2sec

    When I uncheck "Beschafft" for "Status Beschaffung" filter
    Then I see "0 Anträge"

    # Now modify the order status for in item in inspecting phase
    When I check all items for "Budgetperioden" filter
    And I uncheck all items for "Budgetperioden" filter
    And I check "Inspecting" for "Budgetperioden" filter

    And Sleep 2sec
    And I expand the line of the request for "Zebra"
    And I enter the following data into the request form:
      | field                  | value                |
      | Beschaffungs-Status    | Nicht beschafft      |
      | Beschaffungs-Kommentar | Leider nicht möglich |
    And I click on 'Speichern'

    And Sleep 2sec
    And I expand the line of the request for "Zebra"
    Then the request form has the following data:
      | field                  | value                |
      | Beschaffungs-Status    | Nicht beschafft      |
      | Beschaffungs-Kommentar | Leider nicht möglich |
    And I click on 'Abbrechen'

    # Make sure filter reflects the new status
    When I uncheck "Nicht beschafft" for "Status Beschaffung" filter
    Then I see "0 Anträge"

    # Now modify the order status for in item in requesting phase
    When I check all items for "Budgetperioden" filter
    And I uncheck all items for "Budgetperioden" filter
    And I check "Requesting" for "Budgetperioden" filter

    And Sleep 2sec
    And I expand the line of the request for "Gepard"
    And I enter the following data into the request form:
      | field                  | value                 |
      | Beschaffungs-Status    | Alternative beschafft |
      | Beschaffungs-Kommentar | Genausogut            |
    And I click on 'Speichern'

    And Sleep 2sec
    And I expand the line of the request for "Gepard"
    Then the request form has the following data:
      | field                  | value                 |
      | Beschaffungs-Status    | Alternative beschafft |
      | Beschaffungs-Kommentar | Genausogut            |
    And I click on 'Abbrechen'

    # Make sure filter reflects the new status
    When I uncheck "Alternative beschafft" for "Status Beschaffung" filter
    Then I see "0 Anträge"

  Scenario: Viewer can view the order status
    When I log in as the viewer

    Then I see "3 Anträge"

    # Can view order status in procuring phase (but not edit)
    When I uncheck all items for "Budgetperioden" filter
    And I check "Procuring" for "Budgetperioden" filter
    And I expand the line of the main category "Tiere"
    And I expand the line of the category "Säugetiere"
    And I expand the line of the request for "Elefant"
    Then I see "Artikel oder Projekt" in the request form
    And I see "Beschaffungs-Status" in the request form
    And I see "Nicht bearbeitet" in the request form
    And I see a readonly "order_status" field in the request form
    And I click on "Schliessen"

    # Can view order status in inspecting phase (but not edit)
    And I check all items for "Budgetperioden" filter
    And I uncheck all items for "Budgetperioden" filter
    And I check "Inspecting" for "Budgetperioden" filter
    And I expand the line of the request for "Zebra"
    Then I see "Artikel oder Projekt" in the request form
    And I see "Beschaffungs-Status" in the request form
    And I see "Nicht bearbeitet" in the request form
    And I see a readonly "order_status" field in the request form
    And I click on "Schliessen"

    # Can view order status in requesting phase (but not edit)
    And I check all items for "Budgetperioden" filter
    And I uncheck all items for "Budgetperioden" filter
    And I check "Requesting" for "Budgetperioden" filter
    And I expand the line of the request for "Gepard"
    Then I see "Artikel oder Projekt" in the request form
    And I see "Beschaffungs-Status" in the request form
    And I see "Nicht bearbeitet" in the request form
    And I see a readonly "order_status" field in the request form
    And I click on "Schliessen"

  Scenario: Requester can view the order status
    When I log in as the requester "Elvira Ehring"

    Then I see "3 Anträge"

    # Can view order status in procuring phase (but not edit)
    When I uncheck all items for "Budgetperioden" filter
    And I check "Procuring" for "Budgetperioden" filter
    And I expand the line of the main category "Tiere"
    And I expand the line of the category "Säugetiere"
    And I expand the line of the request for "Elefant"
    Then I see "Artikel oder Projekt" in the request form
    And I see "Beschaffungs-Status" in the request form
    And I see "Nicht bearbeitet" in the request form
    And I see a readonly "order_status" field in the request form
    And I click on "Schliessen"

    # Can view order status in inspecting phase
    And I check all items for "Budgetperioden" filter
    And I uncheck all items for "Budgetperioden" filter
    And I check "Inspecting" for "Budgetperioden" filter
    And I expand the line of the request for "Zebra"
    Then I see "Artikel oder Projekt" in the request form
    And I see "Beschaffungs-Status" in the request form
    And I see "Nicht bearbeitet" in the request form
    And I see a readonly "order_status" field in the request form
    And I click on "Schliessen"

    # Can view order status in requesting phase
    And I check all items for "Budgetperioden" filter
    And I uncheck all items for "Budgetperioden" filter
    And I check "Requesting" for "Budgetperioden" filter
    And I expand the line of the request for "Gepard"
    Then I see "Artikel oder Projekt" in the request form
    And I see "Beschaffungs-Status" in the request form
    And I see "Nicht bearbeitet" in the request form
    And I see a readonly "order_status" field in the request form
    And I click on "Abbrechen"
