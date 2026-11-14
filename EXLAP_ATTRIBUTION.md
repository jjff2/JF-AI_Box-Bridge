# EXLAP Attribution and Compatibility

This project refers to publicly documented EXLAP profiles and identifiers from the [EXLAP MIB2 Object Atlas](https://github.com/ijord/EXLAP-mib2-atlas).

Atlas attribution: © 2026 EXLAP MIB2 Object Atlas authors. License: [Creative Commons Attribution 4.0 International (CC BY 4.0)](https://creativecommons.org/licenses/by/4.0/).

The app's EXLAP connection and diagnostic implementation is part of JF AIBox Bridge. The referenced profiles are research material, not an endorsement by any vehicle manufacturer, and do not guarantee that a specific head unit will accept a connection.

Optional local EXLAP settings may be supplied in `local-exlap.properties`, which must not be committed. The app includes published EXLAP reference credentials as defaults. Connection success depends on head-unit compatibility and firmware. Only connect to devices that you own or are authorized to use.

Before choosing a license for the complete repository, review the licenses and redistribution rights of all bundled code and assets. Third-party attribution is not a blanket permission to relicense third-party material.

The default EXLAP profile values in `app/build.gradle` are drawn from the publicly documented reference profiles linked above. They are not unique per-user credentials.
