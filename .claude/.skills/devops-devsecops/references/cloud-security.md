# Cloud Security Reference

## AWS Security

### IAM Best Practices
```json
// NEVER — wildcard everything
{
  "Effect": "Allow",
  "Action": "*",
  "Resource": "*"
}

// CORRECT — least privilege with conditions
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject"
      ],
      "Resource": "arn:aws:s3:::my-bucket/uploads/*",
      "Condition": {
        "StringEquals": {
          "aws:PrincipalTag/team": "engineering"
        },
        "Bool": {
          "aws:SecureTransport": "true"
        }
      }
    }
  ]
}
```

**IAM Rules:**
- No IAM users for applications — use IAM roles with OIDC/AssumeRole
- Enable MFA on all human accounts; enforce via SCP
- Use permission boundaries for delegated admin
- Review with IAM Access Analyzer quarterly
- Use `aws:SourceIp` conditions for admin actions
- Enable credential reports and unused access findings

### S3 Security
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Deny",
      "Principal": "*",
      "Action": "s3:*",
      "Resource": [
        "arn:aws:s3:::my-bucket",
        "arn:aws:s3:::my-bucket/*"
      ],
      "Condition": {
        "Bool": { "aws:SecureTransport": "false" }
      }
    },
    {
      "Effect": "Deny",
      "Principal": "*",
      "Action": "s3:PutObject",
      "Resource": "arn:aws:s3:::my-bucket/*",
      "Condition": {
        "StringNotEquals": {
          "s3:x-amz-server-side-encryption": "aws:kms"
        }
      }
    }
  ]
}
```

**S3 Checklist:**
- [ ] Block Public Access enabled (account-level)
- [ ] Server-side encryption (SSE-KMS) enforced via bucket policy
- [ ] Versioning enabled for critical buckets
- [ ] Access logging enabled
- [ ] Lifecycle policies to delete old versions
- [ ] VPC endpoints for private access
- [ ] Object Lock for compliance data

### VPC Security
```hcl
# Terraform — secure VPC pattern
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = { Name = "production-vpc" }
}

# Private subnets for workloads
resource "aws_subnet" "private" {
  count             = 3
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet("10.0.0.0/16", 8, count.index)
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = { Name = "private-${count.index}" }
}

# Security group — deny by default, explicit allow
resource "aws_security_group" "app" {
  vpc_id = aws_vpc.main.id

  # No inline rules — use aws_security_group_rule for auditability
  tags = { Name = "app-sg" }
}

resource "aws_security_group_rule" "app_ingress" {
  type                     = "ingress"
  from_port                = 443
  to_port                  = 443
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.alb.id
  security_group_id        = aws_security_group.app.id
}

# Flow logs for forensics
resource "aws_flow_log" "vpc" {
  vpc_id          = aws_vpc.main.id
  traffic_type    = "ALL"
  log_destination = aws_cloudwatch_log_group.flow_logs.arn
  iam_role_arn    = aws_iam_role.flow_logs.arn
}
```

### AWS Security Services
| Service | Purpose | Enable? |
|---|---|---|
| **GuardDuty** | Threat detection (API, network, DNS) | Yes, all accounts |
| **Security Hub** | Centralized findings, compliance checks | Yes, aggregate to security account |
| **Config** | Resource inventory, compliance rules | Yes, all regions |
| **CloudTrail** | API audit logging | Yes, org trail, S3 + CloudWatch |
| **IAM Access Analyzer** | Unused access, external sharing | Yes, per account |
| **Macie** | PII/sensitive data discovery in S3 | Yes, for data-sensitive buckets |
| **Inspector** | EC2/Lambda/ECR vulnerability scanning | Yes, continuous |
| **WAF** | Web application firewall | Yes, on ALB/CloudFront/API GW |
| **Shield** | DDoS protection | Standard (free) always; Advanced for critical |
| **KMS** | Key management | Yes, CMKs for sensitive data |

### AWS SCPs (Organization-Level Guardrails)
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "DenyRootAccount",
      "Effect": "Deny",
      "Action": "*",
      "Resource": "*",
      "Condition": {
        "StringLike": { "aws:PrincipalArn": "arn:aws:iam::*:root" }
      }
    },
    {
      "Sid": "DenyLeavingOrg",
      "Effect": "Deny",
      "Action": "organizations:LeaveOrganization",
      "Resource": "*"
    },
    {
      "Sid": "RequireIMDSv2",
      "Effect": "Deny",
      "Action": "ec2:RunInstances",
      "Resource": "arn:aws:ec2:*:*:instance/*",
      "Condition": {
        "StringNotEquals": { "ec2:MetadataHttpTokens": "required" }
      }
    }
  ]
}
```

---

## GCP Security

### IAM
- Use Workload Identity for GKE pods (no service account keys)
- Prefer predefined roles over primitive (viewer/editor/owner)
- Use IAM Conditions for context-aware access
- Enable Organization Policy constraints
- Use VPC Service Controls for data exfiltration prevention

### Key Services
| Service | Purpose |
|---|---|
| **Security Command Center** | Centralized security findings |
| **Chronicle** | SIEM, threat detection |
| **BeyondCorp Enterprise** | Zero-trust access |
| **Cloud Armor** | WAF + DDoS |
| **Cloud KMS** | Key management |
| **Secret Manager** | Secrets storage |
| **Binary Authorization** | Container image attestation |

### GCP Organization Policies
```yaml
# Restrict VM external IPs
constraint: constraints/compute.vmExternalIpAccess
listPolicy:
  allValues: DENY

# Require OS Login
constraint: constraints/compute.requireOsLogin
booleanPolicy:
  enforced: true

# Restrict public bucket access
constraint: constraints/storage.publicAccessPrevention
booleanPolicy:
  enforced: true
```

---

## Azure Security

### Key Services
| Service | Purpose |
|---|---|
| **Microsoft Defender for Cloud** | CSPM + CWPP |
| **Azure Policy** | Compliance and governance |
| **Key Vault** | Secrets + keys + certificates |
| **Azure AD / Entra ID** | Identity + conditional access |
| **Azure Sentinel** | SIEM + SOAR |
| **Azure Firewall** | Network security |
| **NSG Flow Logs** | Network forensics |

### Azure Policy Examples
```json
{
  "if": {
    "allOf": [
      { "field": "type", "equals": "Microsoft.Storage/storageAccounts" },
      { "field": "Microsoft.Storage/storageAccounts/supportsHttpsTrafficOnly", "notEquals": "true" }
    ]
  },
  "then": { "effect": "deny" }
}
```

---

## Multi-Cloud Security Patterns

### Logging & Monitoring
- Centralize logs: CloudTrail/VPC Flow Logs → S3 → SIEM
- Alert on: root/admin logins, IAM changes, security group changes, unusual API calls
- Retain: 90 days hot, 1 year warm, 7 years cold (compliance)
- Use infrastructure-as-code for all alerting rules

### Encryption Standards
| Data State | Method | Key Management |
|---|---|---|
| At rest | AES-256 | Cloud KMS with CMK |
| In transit | TLS 1.2+ | ACM/Let's Encrypt |
| In use | Confidential computing (optional) | HSM-backed keys |

### Network Security
- Private subnets for all workloads; no direct internet access
- NAT Gateway / Cloud NAT for outbound
- VPC endpoints / Private Link for cloud service access
- WAF on all public endpoints
- DDoS protection enabled
- DNS filtering / threat intelligence feeds

### Identity Federation
- Use OIDC / SAML federation — no long-lived cloud credentials
- CI/CD: GitHub OIDC → AWS/GCP/Azure (short-lived tokens)
- Humans: SSO with MFA enforced
- Service-to-service: workload identity / IAM roles, not API keys
