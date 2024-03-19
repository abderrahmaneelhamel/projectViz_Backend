package com.javatechie.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class UserResponse {
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        @Column(name = "id", nullable = false)
        private Long id;

        @Lob
        @Column(columnDefinition = "TEXT")
        private String projectAnalysis;

        @Lob
        @Column(columnDefinition = "TEXT")
        private String techologieRecomendation;

        @Lob
        @Column(name = "classUML", length = 10485760)
        private String classUML;

        @Lob
        @Column(name = "sequenceUML", length = 10485760)
        private String sequenceUML;

        @Lob
        @Column(name = "useCaseUML", length = 10485760)
        private String useCaseUML;

        public UserResponse(String projectAnalysis, String techologieRecomendation, String classUML, String sequenceUML, String useCaseUML) {
                this.projectAnalysis = projectAnalysis;
                this.techologieRecomendation = techologieRecomendation;
                this.classUML = classUML;
                this.sequenceUML = sequenceUML;
                this.useCaseUML = useCaseUML;
        }
}
