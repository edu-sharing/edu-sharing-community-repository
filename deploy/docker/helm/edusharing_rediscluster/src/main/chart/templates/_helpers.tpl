{{- define "edusharing_rediscluster.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_rediscluster.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_rediscluster.fullname" -}}
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

{{- define "edusharing_rediscluster.labels" -}}
{{ include "edusharing_rediscluster.labels.instance" . }}
helm.sh/chart: {{ include "edusharing_rediscluster.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "edusharing_rediscluster.labels.instance" -}}
{{ include "edusharing_rediscluster.labels.app" . }}
{{ include "edusharing_rediscluster.labels.version" . }}
{{- end -}}

{{- define "edusharing_rediscluster.labels.version" -}}
version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end -}}

{{- define "edusharing_rediscluster.labels.app" -}}
app: {{ include "edusharing_rediscluster.fullname" . }}
app.kubernetes.io/name: {{ include "edusharing_rediscluster.fullname" . }}
{{- end -}}
