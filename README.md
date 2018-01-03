# Android-PasswordManager
This is a secure implementation of an Android password manager. It uses fingerprint for the authentication process and both asymmetric and symmetric cryptography to store safely passwords.

The application creates a RSA key pair and a AES key. The RSA key pair is used to encrypt/decrypt the symmetric key, that is used to encrypt user data.

The RSA encryption is in ECB/OAEPWITHSHA-256ANDMGF1PADDING mode, while all passwords are encrypted with AES in CBC/PKCS5Padding mode.
Since CBC mode requires an IV for encryption/decryption, the IV parameter is encrypted again with the symmetric key in ECB/PKCS5Padding mode.

<img src="https://github.com/adricarda/Android-PasswordManager/blob/master/auth.png" width="300" height="490">
<img src="https://github.com/adricarda/Android-PasswordManager/blob/master/home.png" width="300" height="490">
<img src="https://github.com/adricarda/Android-PasswordManager/blob/master/show.png" width="300" height="490">
