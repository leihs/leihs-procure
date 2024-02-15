Feature: Verify image-icon for main-category

  Background:
    Given there is an initial admin
    And there are procurement settings

  Scenario: "Create a main category and verify that image-icon has loaded correctly"
    Given there is a budget period "Budget-Period-BP" in inspection phase
    And there is a main category "Main Category MC1"
    And there is category "Category C1" for main category "Main Category MC1"

    And there is a requester
    And there is a requester "The Requester"
    And there is a procurement admin


    When I log in as the procurement admin
    And I open the AdminKategorien menu
    And I click add main-category

    And I enter the following data category form:
      | field          | value              |
      | Bild           | icon-image.png     |
      | Hauptkategorie | Main-Category Test |
    And I click on 'Speichern'

    And I click category "Main-Category Test"
    And I see image-icon with correct response-status

  Scenario: Create main-category & category and verify if image-icon has loaded correctly
    Given there is a budget period "Budget-Period-BP" in inspection phase
    And there is a main category "Main Category MC1"
    And there is category "Category C1" for main category "Main Category MC1"

    And there is a requester
    And there is a requester "The Requester"
    And there is a procurement admin

    When I log in as the procurement admin
    And I open the AdminKategorien menu
    And I click add main-category

    And I enter the following data category form:
      | field             | value              |
      | Bild              | icon-image.png     |
      | Hauptkategorie    | Main-Category Test |
      | Subkategorien     | Category Test      |
      | Kostenstelle      | Cost-Center        |
      | Sachkonto ⅠI (IR) | Sachkonto 2 Value  |
      | Sachkonto I (ER)  | Sachkonto 1 Value  |
    And I click on 'Speichern'

    And I click category "Main-Category Test"
    And I see image-icon with correct response-status
    And I see input-field for image-upload

  Scenario: Create main-category without image
    Given there is a budget period "Budget-Period-BP" in inspection phase
    And there is a main category "Main Category MC1"
    And there is category "Category C1" for main category "Main Category MC1"

    And there is a requester
    And there is a requester "The Requester"
    And there is a procurement admin

    When I log in as the procurement admin
    And I open the AdminKategorien menu

    And I click add main-category
    And I click add category
    And I enter the following data category form:
      | field             | value             |
      | Hauptkategorie    | MainCategory      |
      | Subkategorien     | Category Test     |
      | Kostenstelle      | Cost-Center       |
      | Sachkonto ⅠI (IR) | Sachkonto 2 Value |
      | Sachkonto I (ER)  | Sachkonto 1 Value |
    And I click on 'Speichern'

    And I click category "MainCategory"
    And I see input-field for image-upload
    And I see no image
