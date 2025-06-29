# Open Schedule

<p align="center">
  <img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License">
  <img src="https://img.shields.io/badge/version-1.0.3-brightgreen.svg" alt="Version">
  <!-- TODO: Add a GitHub Actions build status badge once CI is set up -->
  <!-- <img src="https://github.com/your-username/open-schedule/actions/workflows/build.yml/badge.svg" alt="Build Status"> -->
</p>

A modern, open-source conference and event schedule management application. Built with LOVE in Java, Spring Boot, and
Vaadin, Open Schedule provides a seamless experience for both event organizers and attendees.

<!-- TODO: Add a screenshot or GIF of the application in action -->
<!-- <p align="center">
  <img src="assets/app-screenshot.png" alt="Application Screenshot" width="700"/>
</p> -->

## About The Project

Open Schedule was created to provide a free, robust, and easy-to-deploy solution for managing event agendas. Whether
you're running a small meetup or a multi-track conference, this tool helps you organize sessions, speakers, and rooms,
while offering attendees a clean and interactive interface to explore the schedule and rate sessions.

**Built With:**

* Java & Spring Boot
* Vaadin Flow
* PostgreSQL
* Flyway for database migrations
* Docker & Docker Compose
* Traefik as a reverse proxy

## Features

### Current Features

* **Admin Dashboard:** A secure area for event organizers to manage all aspects of the event.
* **Session Management:** Full CRUD (Create, Read, Update, Delete) functionality for event sessions.
* **Speaker Profiles:** Manage speaker information, including bios, photos, and assigned sessions.
* **Room & Track Organization:** Easily assign sessions to specific rooms and thematic tracks.
* **Tagging System:** Use tags to categorize sessions for better filtering and discovery.
* **Interactive Public Schedule:** A clean, user-friendly view for attendees to browse the agenda.
* **Live Session Rating:** Attendees can rate sessions in real-time, providing valuable feedback.
* **Dockerized Deployment:** Comes with docker-compose files for easy setup in both development and production
  environments.
* **PWA (Progressive Web App):** Enhanced mobile experience.

### Future Roadmap & Potential Features

We are always looking to improve! Here are some features we're thinking about, and we
welcome contributions:

* **Personalized Schedules:** Allow attendees to "star" or "favorite" sessions to build their own custom agenda.
* **Internationalization (i18n) Support:** Add the ability to translate the UI into multiple languages.
* **Multi-Event Support:** Manage multiple events from a single instance of the application.
* **Sponsor Management:** A dedicated section to showcase event sponsors.
* **Enhanced Analytics:** A dashboard for admins to view session popularity, ratings, and other key metrics.

## Getting Started

Getting a local instance of Open Schedule running is straightforward.

### Prerequisites

* Java 21 or later
* Apache Maven
* Docker and Docker Compose

### Development Setup (Recommended)

Getting started is simple thanks to the provided `Makefile`, but it's important to understand the two main workflows for
an optimal development experience.

This guide explains the two main workflows for an optimal development experience.

**1. Clone the repository:**

```shell
git clone https://github.com/your-username/open-schedule.git
cd open-schedule
```

**2. First-Time Setup**

The `make restart` command is the recommended way to start the project for the first time. It's a "one-shot" command
that handles everything for you:

* Builds the application's Docker image.
* Launches the complete environment (application, database, and mail server) using Docker Compose.

```shell
# This command builds the image and starts all services
make restart 
```

After this, you can access the application and services:

* Application: http://localhost:51675
* MailHog (Email Catcher): http://localhost:8025

**3. Day-to-Day Development (Faster Workflow)**

After the initial setup, you can take advantage of Vaadin's hot-reloading features for a much faster development cycle.
This involves running the application directly from your IDE (like IntelliJ or VSCode) and using Docker only for the
background services.

* **Ensure background services are running:** If you've stopped everything, you can start just the database and mail
  server with:

```shell
    docker compose up -d postgres mailhog
```

* **Run the application from your IDE:** Open the project in your favorite IDE and run the `main` method in the
  `Application.java` class. The application will start much faster and will automatically connect to the services
  running in Docker. Any changes you make to the frontend or backend code will be reflected almost instantly without
  needing to rebuild the Docker image.

## Configuration & Branding

You can easily customize the application for your event.

### Event Website Link

To link to your event's official website from the main menu, set the following environment variable in your
`docker-compose.ym`l or deployment environment:

```yml
environment:
  - EVENT_WEBSITE=https://your-event-website.com
  ```

### Event Logo

Event LogoTo display your event's logo in the application header:

1. Create a logo file named `logo.png`
2. .Place it inside the `assets/` directory at the root of the project.
3. This directory is mounted as a volume in the Docker container, and the application will automatically pick it up.

## Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any
contributions you make are **greatly appreciated**.

If you have a suggestion that would make this better, please fork the
repo and create a pull request. You can also simply open an issue with the tag "enhancement".

1. **Fork** the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

Distributed under the MIT License. See `LICENSE` for more information.

## About This Project

Open Schedule was born from a passion for community and technology. The idea was sparked during the organization of
[JconfDominicana 2025](https://jconfdominicana.org/), a community-driven Java conference, where we saw a need for a
modern, free, and open-source tool to
manage event schedules without the complexity or cost of commercial solutions.

Our mission is to provide a beautiful, easy-to-use platform that empowers event organizers and delights attendees.

## Maintained By

Open Schedule is proudly developed and maintained by [Alphnology](https://alphnology.com/).

![image](https://github.com/user-attachments/assets/2148a45c-c922-4e51-8f96-ca492409f7c1)
