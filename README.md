🅿️ bPark - Automated Parking Management System

bPark is a smart parking management solution built on a Client-Server architecture. It is designed to streamline parking operations for both customers (occasional and subscribers) and parking lot employees. The system handles real-time reservations, parking space management, automated reporting, and payment processing.
🚀 Key Features

    User & Subscription Management: Comprehensive registration for monthly/annual subscribers and one-time customers.

    Advance Reservations: A user-friendly interface to select specific parking lots, dates, and times, with real-time availability validation.

    Automated Lot Management: An intelligent algorithm to manage parking space statuses (Available/Occupied/Reserved) and update lot capacity dynamically.

    Networked Architecture: Based on TCP/IP communication using the OCSF (Open Client Server Framework).

    Multi-threading: The server is engineered to handle dozens of concurrent client requests simultaneously without freezing the Graphical User Interface (GUI).

    Management Reports: Automated generation of management data including occupancy rates, revenue tracking, and customer complaint logs.

🛠 Tech Stack

    Language: Java 17+

    UI Framework: JavaFX (with Scene Builder)

    Networking: OCSF Framework (TCP/IP)

    Database: MySQL (via JDBC Connector)

    Build Tool: Maven

🏗 Architecture & Design Patterns

The system was developed following advanced software engineering principles to ensure maintainability and scalability:

    ECB Pattern (Entity-Control-Boundary): Complete separation between the UI (Boundary), business logic (Control), and data structures (Entity).

    Singleton: Used to manage a single, consistent connection to the database and the server instance.

    Client-Server Architecture: Ensures a clean decoupling of the front-end (Client) and back-end processing (Server).

    Observer Pattern: Utilized to update GUI components automatically whenever a new message or update is received from the server.

📂 Project Structure
Plaintext

bPark-system/
│
├── bPark-Server/              # Server-side logic & DB management
│   ├── src/main/java/server/
│   │   ├── EchoServer.java    # Handles incoming connections
│   │   └── DBController.java  # MySQL queries & DB logic
│   └── resources/             # SQL schemas & configurations
│
├── bPark-Client/              # UI & Client communication
│   ├── src/main/java/client/
│   │   ├── ClientUI.java      # Entry point
│   │   └── ChatClient.java    # Server communication bridge
│   ├── src/main/java/gui/     # JavaFX Controllers
│   └── src/main/resources/    # FXML files (UI Design)
│
└── Common/                    # Shared Data Entities
    └── src/main/java/logic/   # Order, Car, Parking, and User classes

⚙️ Setup & Installation

    Database Setup:

        Import the provided SQL file located in the resources folder into your MySQL server.

        Update the database credentials (User/Password) in DBController.java.

    Run the Server:

        Launch the Server JAR or run EchoServer.java.

        Enter the desired Port (e.g., 5555).

    Run the Client:

        Launch the Client JAR.

        Enter the Server IP (localhost for local testing) and the Port.

👤 Contact

Fuad Abbas

    Email: Foaad.Abbas@e.braude.ac.il

    LinkedIn: linkedin.com/in/fuad-abbas-910843217

    GitHub: github.com/FoaadAbbas

This project was developed as part of the B.Sc. in Software Engineering at Braude College of Engineering.
