{{- define "edusharing_services_rendering.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_services_rendering.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_services_rendering.labels" -}}
{{ include "edusharing_services_rendering.labels.instance" . }}
helm.sh/chart: {{ include "edusharing_services_rendering.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "edusharing_services_rendering.labels.instance" -}}
{{ include "edusharing_services_rendering.labels.app" . }}
{{ include "edusharing_services_rendering.labels.version" . }}
{{- end -}}

{{- define "edusharing_services_rendering.labels.version" -}}
version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end -}}

{{- define "edusharing_services_rendering.labels.app" -}}
app: {{ include "edusharing_services_rendering.name" . }}
app.kubernetes.io/name: {{ include "edusharing_services_rendering.name" . }}
{{- end -}}

{{- define "edusharing_services_rendering.pvc.share.config" -}}
share-config-{{ include "edusharing_services_rendering.name" . }}
{{- end -}}

{{- define "edusharing_services_rendering.pvc.share.data" -}}
share-data-{{ include "edusharing_services_rendering.name" . }}
{{- end -}}

{{- define "edusharing_services_rendering.image" -}}
{{- $registry := default .Values.global.image.registry .Values.image.registry -}}
{{- $repository := default .Values.global.image.repository .Values.image.repository -}}
{{ $registry }}{{ if $registry }}/{{ end }}{{ $repository }}{{ if $repository }}/{{ end }}
{{- end -}}