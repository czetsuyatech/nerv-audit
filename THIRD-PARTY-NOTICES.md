THIRD-PARTY SOFTWARE NOTICES AND INFORMATION
nerv-audit Pro — Czetsuya Tech
April 2026

This product incorporates components from the following open-source projects.
Each component is governed by its respective license, reproduced or referenced below.

-------------------------------------------------------------------------------

1. Spring Boot
   Version:    4.0.4
   Artifacts:  spring-boot-starter-parent, spring-boot-autoconfigure,
               spring-boot-starter-web, spring-boot-starter-data-jpa,
               spring-boot-starter-validation, spring-boot-starter-liquibase,
               spring-boot-starter-test, spring-boot-webmvc-test,
               spring-boot-starter-data-jpa-test
   Copyright:  Copyright 2012–2026 the original author or authors (VMware, Inc.)
   License:    Apache License, Version 2.0
   URL:        https://spring.io/projects/spring-boot
   License URL: https://www.apache.org/licenses/LICENSE-2.0

-------------------------------------------------------------------------------

2. Spring Framework
   Artifacts:  spring-context, spring-web, spring-webmvc, spring-data-jpa, spring-orm
               (pulled transitively via Spring Boot)
   Copyright:  Copyright 2002–2026 the original author or authors (VMware, Inc.)
   License:    Apache License, Version 2.0
   URL:        https://spring.io/projects/spring-framework
   License URL: https://www.apache.org/licenses/LICENSE-2.0

-------------------------------------------------------------------------------

3. Spring Security
   Version:    (managed by Spring Boot BOM)
   Artifact:   spring-security-core
   Copyright:  Copyright 2004–2026 the original author or authors (VMware, Inc.)
   License:    Apache License, Version 2.0
   URL:        https://spring.io/projects/spring-security
   License URL: https://www.apache.org/licenses/LICENSE-2.0

-------------------------------------------------------------------------------

4. Hibernate ORM
   Version:    (managed by Spring Boot BOM)
   Artifacts:  hibernate-core, hibernate-envers
   Copyright:  Copyright 2001–2026 Red Hat, Inc. and/or its affiliates
   License:    GNU Lesser General Public License, Version 2.1
   URL:        https://hibernate.org/orm
   License URL: https://www.gnu.org/licenses/lgpl-2.1.html

   NOTE: LGPL 2.1 permits use and linking in proprietary software provided
   that the LGPL-licensed component is used in unmodified form or that
   modifications are made available. nerv-audit Pro does not modify Hibernate ORM
   or Hibernate Envers source code.

-------------------------------------------------------------------------------

5. Lombok
   Version:    (managed by Spring Boot BOM)
   Artifact:   lombok
   Copyright:  Copyright 2009–2026 The Project Lombok Authors
   License:    MIT License
   URL:        https://projectlombok.org
   License URL: https://opensource.org/licenses/MIT

   Permission is hereby granted, free of charge, to any person obtaining a copy
   of this software and associated documentation files (the "Software"), to deal
   in the Software without restriction, including without limitation the rights
   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
   copies of the Software, and to permit persons to whom the Software is
   furnished to do so, subject to the following conditions:

   The above copyright notice and this permission notice shall be included in
   all copies or substantial portions of the Software.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.

-------------------------------------------------------------------------------

6. H2 Database Engine (test scope only — not distributed in production artifacts)
   Version:    (managed by Spring Boot BOM)
   Artifact:   h2
   Copyright:  Copyright 2004–2026 H2 Group
   License:    Eclipse Public License 2.0 / Mozilla Public License 2.0 (dual-licensed)
   URL:        https://h2database.com
   License URL: https://h2database.com/html/license.html

   H2 is used exclusively as a test dependency. It is not included in or
   distributed with production binaries of nerv-audit Pro.

-------------------------------------------------------------------------------

7. Liquibase (test scope only — not distributed in production artifacts)
   Version:    5.0.2
   Artifact:   liquibase-core (via spring-boot-starter-liquibase)
   Copyright:  Copyright 2006–2026 Liquibase Inc.
   License:    Apache License, Version 2.0
   URL:        https://www.liquibase.com
   License URL: https://www.apache.org/licenses/LICENSE-2.0

   Liquibase is used exclusively as a test dependency. It is not included in or
   distributed with production binaries of nerv-audit Pro.

-------------------------------------------------------------------------------

APACHE LICENSE, VERSION 2.0 (SUMMARY)

The Apache License 2.0 grants a perpetual, worldwide, non-exclusive, royalty-free
license to reproduce, prepare derivative works of, publicly display, sublicense,
and distribute the licensed work and such derivative works in source or object form,
subject to the following conditions:

  - You must give any other recipients a copy of the Apache License 2.0.
  - You must cause any modified files to carry prominent notices stating that
    you changed the files.
  - You must retain all copyright, patent, trademark, and attribution notices.
  - If the licensed work includes a NOTICE file, you must include a readable
    copy of the attribution notices contained therein.

Full license text: https://www.apache.org/licenses/LICENSE-2.0

-------------------------------------------------------------------------------

© 2026 Czetsuya Tech. All rights reserved.
nerv-audit Pro proprietary source code and compilation are NOT covered by the
licenses listed above. See LICENSE.md for terms governing nerv-audit Pro.
