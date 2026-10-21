import { Component, Input } from '@angular/core';
@Component({selector:'app-stat-card',standalone:false,template:`<mat-card class="stat"><div class="stat-top"><span>{{label}}</span><span class="stat-icon" [ngClass]="tone"><mat-icon>{{icon}}</mat-icon></span></div><strong>{{suffix?value:(value|number:'2.0')}}@if(suffix){<em>{{suffix}}</em>}</strong><small>@if(live){<span class="dot"></span>}{{note}}</small></mat-card>`})
export class StatCardComponent {@Input()label='';@Input()value=0;@Input()suffix='';@Input()note='';@Input()icon='groups';@Input()tone='mint';@Input()live=false;}
