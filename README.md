# Mr Tasks

**A powerful task management application designed for freelancers, small businesses, and professionals.**

Mr Tasks is a comprehensive task and client management platform that helps you stay organized and productive. Built with user experience in mind, it offers intuitive task tracking, client management, invoicing, and detailed reporting capabilities.

## 🚀 Features

### Core Functionality
- **Task Management**: Drag-and-drop task organization with status tracking
- **Client Management**: Comprehensive client profiles and contact management
- **Invoice Generation**: Professional PDF invoices with multi-language support
- **Reporting & Analytics**: Detailed reports and revenue trend analysis
- **Calendar Integration**: Task scheduling and deadline management

### User Experience
- **Multi-language Support**: Available in English, Greek, Spanish, French, German, and Italian
- **Responsive Design**: Works seamlessly on desktop and mobile devices
- **OAuth2 Integration**: Login with Google for quick access
- **Real-time Validation**: Client-side form validation for better UX

### Business Features
- **Payment Processing**: Integrated Stripe payment handling
- **Email Notifications**: Automated email for invoices and notifications
- **User Profiles**: Customizable user and company profiles
- **Admin Panel**: Administrative tools for user management
- **Blog System**: Built-in blog functionality

## 🛠️ Technology Stack

### Backend
- **Spring Boot 3.2.4** - Main application framework
- **Java 21** - Programming language
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database access layer
- **PostgreSQL** - Primary database
- **OpenPDF** - PDF generation for invoices

### Frontend
- **Thymeleaf** - Server-side templating
- **Bootstrap** - CSS framework
- **JavaScript** - Client-side interactivity
- **jQuery** - DOM manipulation

### Integration & Services
- **OAuth2** - Google authentication
- **Stripe** - Payment processing
- **Spring Mail** - Email functionality
- **Bucket4j** - Rate limiting

### Build & Deployment
- **Maven** - Build management
- **Node.js/npm** - Frontend asset processing
- **Heroku** - Cloud deployment ready
- **AWS Elastic Beanstalk** - Alternative deployment option

## 📋 Prerequisites

Before running Mr Tasks, ensure you have the following installed:

- **Java 21** or higher
- **Maven 3.6+**
- **Node.js 22.15.0+** and **npm 11.3.0+**
- **PostgreSQL 12+**

## 🔧 Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/stafidas/mrtasks.git
cd mrtasks
```

### 2. Database Setup
Create a PostgreSQL database and user:
```sql
CREATE DATABASE taskmaster;
CREATE USER taskmaster_user WITH ENCRYPTED PASSWORD 'taskmaster123';
GRANT ALL PRIVILEGES ON DATABASE taskmaster TO taskmaster_user;
```

### 3. Environment Configuration
Create environment variables or update `src/main/resources/application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/taskmaster
spring.datasource.username=taskmaster_user
spring.datasource.password=taskmaster123

# OAuth2 Configuration
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# Stripe Configuration
STRIPE_API_KEY=your_stripe_api_key
STRIPE_WEBHOOK_SECRET=your_stripe_webhook_secret

# Email Configuration
EMAIL_SERVER=your_smtp_server
EMAIL_USERNAME=your_email_username
EMAIL_PASSWORD=your_email_password

# reCAPTCHA Configuration
RECAPTCHA_SECRET=your_recaptcha_secret
```

### 4. Build the Application
```bash
# Ensure Java 21 is active
export JAVA_HOME=/path/to/java21
export PATH=$JAVA_HOME/bin:$PATH

# Build the application
mvn clean compile
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`

## 🚀 Deployment

### Heroku Deployment
The application is ready for Heroku deployment with the included `Procfile`:

```bash
# Login to Heroku
heroku login

# Create Heroku app
heroku create your-app-name

# Set environment variables
heroku config:set GOOGLE_CLIENT_ID=your_value
heroku config:set GOOGLE_CLIENT_SECRET=your_value
heroku config:set STRIPE_API_KEY=your_value
# ... set other required environment variables

# Deploy
git push heroku main
```

### AWS Elastic Beanstalk
Configuration files are included in `.ebextensions/` for AWS deployment.

## 📱 Usage

### Getting Started
1. **Register**: Create a new account or login with Google
2. **Profile Setup**: Complete your profile with company information
3. **Add Clients**: Create client profiles for your customers
4. **Create Tasks**: Add tasks and assign them to clients
5. **Generate Invoices**: Select completed tasks to create professional invoices
6. **Track Progress**: Use the dashboard and reporting features to monitor your business

### Key Workflows
- **Task Management**: Create, update, and track task status
- **Client Billing**: Generate and send invoices to clients
- **Business Analytics**: View reports and revenue trends
- **Calendar Planning**: Schedule tasks and manage deadlines

## 🛡️ Security Features

- **CSRF Protection**: Cross-site request forgery protection
- **Session Management**: Secure session handling with timeouts
- **Password Encryption**: BCrypt password hashing
- **Rate Limiting**: API rate limiting to prevent abuse
- **OAuth2 Integration**: Secure third-party authentication

## 🌐 Internationalization

Mr Tasks supports multiple languages:
- English (default)
- Greek (Ελληνικά)
- Spanish (Español)
- French (Français)
- German (Deutsch)
- Italian (Italiano)

Language preferences are automatically detected and can be changed in user settings.

## 🤝 Contributing

We welcome contributions to Mr Tasks! Here's how you can help:

### Development Setup
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes and commit: `git commit -m 'Add amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Code Style
- Follow Java coding conventions
- Use meaningful variable and method names
- Include comments for complex logic
- Ensure responsive design for frontend changes

### Testing
- Write unit tests for new features
- Test across different browsers and devices
- Verify multi-language functionality

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

- **Email**: support@mrtasks.com
- **Website**: [mrtasks.com](https://mrtasks.com)
- **Issues**: Use GitHub Issues for bug reports and feature requests

## 🙏 Acknowledgments

- Built with Spring Boot and the Spring ecosystem
- UI components powered by Bootstrap
- PDF generation using OpenPDF
- Payment processing by Stripe
- Icons provided by Bootstrap Icons

---

**© 2025 Mr Tasks. All rights reserved.**