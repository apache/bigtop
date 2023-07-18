Getting started with bigtop packaging.

1) You will have to package rpm.

2) Here are some steps you can follow to bring a new bigtop package in.

determine where your source is, and add it to bigtop.bom
update the bigtop-packages/src/common/<your-package> folder to have your component, and the do-component-build for it (which usually just builds a jar).  Why is there a "common" directory? Simply because deb and rpm packaging share some tasks (like do-component-build, which just usually runs a mvn or gradle command), and so we keep a common install directory which they can both leverage for packaging.
(for RPM) now add a .spec file into bigtop-packages/src/<your-package>/... into the appropriate directory (i.e. bigtop-packages/src/rpm/tachyon/SPECS/tachyon.spec).  Obviously, your tachyon.spec file will use whats in common/ in a RPM specific way, to install the RPM package.
create a rules file using do-component-build

Test it with gradle <your-package>-rpm for the others.
Finally add a smoke test! This is as easy as adding a new groovy file to bitop-tests/smoke-tests/<your-package>/TestThisStuff.groovy, following conventions that others have created.
3) As always, we will improve on the directions above, but this should help to get you started. .