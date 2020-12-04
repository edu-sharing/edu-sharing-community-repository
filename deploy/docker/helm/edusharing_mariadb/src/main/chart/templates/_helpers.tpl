{{- define "edusharing_mariadb.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_mariadb.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_mariadb.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "edusharing_mariadb.labels" -}}
{{ include "edusharing_mariadb.labels.instance" . }}
helm.sh/chart: {{ include "edusharing_mariadb.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "edusharing_mariadb.labels.instance" -}}
{{ include "edusharing_mariadb.labels.app" . }}
{{ include "edusharing_mariadb.labels.version" . }}
{{- end -}}

{{- define "edusharing_mariadb.labels.app" -}}
app: {{ include "edusharing_mariadb.fullname" . }}
app.kubernetes.io/name: {{ include "edusharing_mariadb.fullname" . }}
{{- end -}}

{{- define "edusharing_mariadb.labels.version" -}}
version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end -}}
