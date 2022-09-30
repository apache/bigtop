# Kerberos Ambari wizard

> Make sure your service is up and running

### 1. Enabled Kerberos
![Kerberos_Ambari_wizard_1](images/Kerberos_Ambari_wizard_1.png)
Then, enter `ENABLE KERBEROS` button.

### 2. Choose `Existing MIT KDC`
![Kerberos_Ambari_wizard_2](images/Kerberos_Ambari_wizard_2.png)
Then, enter `NEXT` button.

### 3. Fill in the KDC account password
```bash
# #############################################
#                  KDC                        #
# KDC hosts      : ambari-server              #
# Realm name     : EXAMPLE.COM                #
# #############################################
#                 Kadmin                      #
# Kadmin host    : ambari-server              #
# Admin principal: admin/admin@EXAMPLE.COM    #
# Admin password : admin                      #
# #############################################
```
Admin password is `admin`
![Kerberos_Ambari_wizard_3](images/Kerberos_Ambari_wizard_3.png)

### 4. Remember to comment this line for high version of JDK 1.8

> Edit `Advanced krb5-conf` -> `krb5-conf template` -> `[libdefaults]` and comment `renew_lifetime = 7d`

![Kerberos_Ambari_wizard_4](images/Kerberos_Ambari_wizard_4.png)
Then, enter `NEXT` button.

### 5. Install and Test Kerberos Client

![Kerberos_Ambari_wizard_5](images/Kerberos_Ambari_wizard_5.png)
Then, enter `NEXT` button.

### 6. Check the Kerberos configuration

![Kerberos_Ambari_wizard_6](images/Kerberos_Ambari_wizard_6.png)
Then, enter `NEXT` button.

### 7.  Confirm Configuration

![Kerberos_Ambari_wizard_7](images/Kerberos_Ambari_wizard_7.png)
Then, enter `NEXT` button.

### 8. Stop Service

![Kerberos_Ambari_wizard_8](images/Kerberos_Ambari_wizard_8.png)
Then, enter `NEXT` button.

### 9. Kerberize Cluster

![Kerberos_Ambari_wizard_9](images/Kerberos_Ambari_wizard_9.png)
Then, enter `NEXT` button.

### 10. Start and Test Services

![Kerberos_Ambari_wizard_10](images/Kerberos_Ambari_wizard_10.png)
Then, enter `NEXT` button.

### 11. Kerberos successfully enabled
![Kerberos_Ambari_wizard_11](images/Kerberos_Ambari_wizard_11.png)
![Kerberos_Ambari_wizard_12](images/Kerberos_Ambari_wizard_12.png)