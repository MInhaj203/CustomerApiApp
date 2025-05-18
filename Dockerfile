# Use the official Tomcat base image with JDK 17
FROM tomcat:10.1-jdk17

# Remove default web apps (optional)
RUN rm -rf /usr/local/tomcat/webapps/*

# Copy WAR file into the ROOT webapp location
COPY target/customerapiapp.war /usr/local/tomcat/webapps/ROOT.war


# Expose port 8080
EXPOSE 8080

# Start Tomcat
CMD ["catalina.sh", "run"]

