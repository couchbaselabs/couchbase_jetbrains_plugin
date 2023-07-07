# Intellij-Couchbase README

Welcome to the official Intellij Idea extension for Couchbase!

This plugin is designed to provide a seamless experience for Couchbase Server users who want to work within the popular Intellij Idea editor.

# Requirements
This extension requires OpenSSL to be installed on your system in order to install successfully. 

For Windows:
- Download the latest version of OpenSSL from https://slproweb.com/products/Win32OpenSSL.html and follow the installation instructions.

For Debian or Ubuntu-based distros::
- Run `sudo apt-get install openssl`

For Red Hat, CentOS or Fedora-based distros:
-  Run `sudo dnf install openssl` (on newer Fedora versions), OR `sudo yum install openssl` (on older Fedora/Red Hat/CentOS versions)

For Mac:
- For an Apple device with an M1 chip, you should install OpenSSL version 3 or higher. On the other hand, if your Mac has an Intel (x64) chip, then you should install OpenSSL version 1.1.
- Run `brew install openssl` using Homebrew if your Mac has M1 chip and use `brew install openssl@1.1` if your Mac has an Intel (x64), or download the latest version of OpenSSL from https://www.openssl.org/source/ and follow the installation instructions.

In case you encounter any issues during the installation process, we have provided detailed [Troubleshooting Guidelines](https://docs.google.com/document/d/132jKt2YFGjhzIM0yMUi3l5VjC3FQ-rL5-xUiTRLYKds/edit?usp=sharing) to help you resolve the issues. 

To install Couchbase Server, please follow the steps mentioned in [this guide](https://docs.couchbase.com/server/current/install/install-intro.html).

> :warning:  **WARNING**:  DO NOT upgrade cmake-js to 7.x or it will break Windows builds. Please use 6.x instead. 

# Quick Tour

### Add Cluster Connection
To connect to a Couchbase Cluster, follow these steps:
1. Click on the "Add" icon.
2. Complete all the required fields with the necessary details.
3. Check "Use Secure Connection" if you are connecting to Capella.
4. Finally, click on the "Connect" button to establish a connection to the cluster.

<img src="gifs/AddConnection.gif" height="80%" width="80%" alt="Add Connection to Cluster" />

### Connect, Disconnect or Delete a Cluster Connection
Right-click on the connection to open the context menu. From the context menu, you will be able to choose between options to Connect, Disconnect or Delete the Cluster.

<img src="gifs/ConnectDisconnectCluster.gif" height="80%" width="80%" alt="Connect/Disconnect to Cluster" />

### Interact with Buckets and Scopes
Click on the bucket to see the list of Scopes associated with that bucket. Open the context menu on bucket to undertake actions such as creating a new Scope, refreshing existing Scopes, or obtaining administrative information about the Bucket.

<img src="gifs/InteractWithBucketsScopes.gif" height="80%" width="80%" alt="Interact with Buckets and Scopes" />

### Interact with Collections and Indexes
Click on Scope to list Collections and Indexes. Open context menu on Collection directory to create a new Collection or to refresh Collections.

<img src="gifs/InteractWithCollectionIndexes.gif" height="80%" width="80%" alt="Interact with Collections and Indexes" />


### Interact with Documents
1. Click on the desired Collection to list all the documents that have been stored within it.
2. To open a specific document, click on it to view its details and make any necessary changes.
3. If you need to create a new document or search for an existing one, you can open the context menu by right-clicking on the Collection directory.
4. Once you have made any necessary changes to a document, you can save it using the "Ctrl + S" or "Cmd + S" shortcut keys.

<img src="gifs/InteractWithDocuments.gif" height="80%" width="80%" alt="Interact with Documents" />

### Open SQL++ Notebook
1. Simply right-click on the Cluster and select the "New SQL++ Notebook" option from the context menu.
2. Once you have opened the notebook, you will be presented with a powerful text editor that allows you to craft your SQL++ queries with ease.
3. And once you have completed your work, you can save the notebook to your local machine for future reference.

<img src="gifs/QueryNotebook.gif" height="80%" width="80%" alt="Interact with Documents" />

## License
Apache Software License Version 2.  See individual files for details.