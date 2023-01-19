# A-QuB-2 | A Web Application for Semantic Data Exploration

## System Description ##

A-QuB-2 is a web application that facilitates the exploration of semantic (RDF) data by plain users. It offers a user-friendly interface that supports users in building and running complex queries step-by-step, without needing to know and write SPARQL. Its configuration model allows setting up the application for use over any knowledge base, independent of the underlying data model / ontology. 

The supported functionalities include:
* Assistive query building interface that exploits the (pre-configured) relationships among the involved entities
* Data filtering based on entity, text, number range, date range, boolean value, map area 
* Results visualisation in table
* Results browsing by exploiting the entity interconnections
* Results download in CSV format for furthe (offline) analysis

The system is an extension of a previous version (called A-QuB), described in the following demo paper:
Kritsotakis et al. "Assistive Query Building for Semantic Data." In SEMANTICS Posters&Demos. 2018 ([PDF](https://ceur-ws.org/Vol-2198/paper_107.pdf)).
The current version a) implements a configuration model that allows fully configuring the application through a properties file, b) has been extended for supporting additional functionalities, such as filtering  by number range, boolean value, date range (including expressions like '20th century', 'decade of 1970', etc.), and others.

## Technologies Used

- **Spring Boot** – A project built on the top of the Spring framework. It provides a simpler and faster way to set up, configure, and run both simple and web-based (Spring Web MVC) applications;
- **AngularJS** - A structural framework for dynamic web apps based on HTML and JavaScript;
- **Material Design & Bootstrap-UI** - UI component frameworks;
- **H2** – A relational database management system written in Java, that can be embedded in Java applications;

## Contact ##

- Fafalios Pavlos (<fafalios@ics.forth.gr>)


## Acknowledgements ##

The current version of this work has received funding from the European Union's Horizon 2020 research and innovation programme under the Marie Sklodowska-Curie grant agreement No 890861 ([Project ReKnow](https://reknow.ics.forth.gr/)).

The previous version of this work was carried out within the H2020 project [VRE4EIC](https://cordis.europa.eu/project/id/676247) (No 676247). 
