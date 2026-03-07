package com.example.app.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "FVMADM_SERVER_CONFIG")
public class ServerConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "HOST_NAME")
    private String hostName="";

    @Column(name = "SSH_PORT")
    private String sshPort;

    @Column(name = "PATH_LIST")
    private String pathList="";

    @Column(name = "USERNAME")
    private String userName="";

    @Column(name = "SSH_KEY")
    private String sshKey="";

    @Column(name = "HOST_ALIAS")
    private String hostAlias="";

}
